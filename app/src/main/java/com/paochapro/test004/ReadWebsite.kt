package com.paochapro.test004

import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.Calendar
import java.util.GregorianCalendar

fun readWebsiteAndStoreInSchedule(activity: MainActivity, login: String, password: String) {
    val builder: OkHttpClient.Builder = OkHttpClient.Builder()
    builder.cookieJar(PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(activity)))
    val client: OkHttpClient = builder.build()

    // Try to login
    val loginUrl = "https://elschool.ru/Logon/Index"

    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("login", login)
        .addFormDataPart("password", password)
        .build()

    val loginRequest: Request = Request.Builder()
        .url(loginUrl)
        .post(body)
        .build()

    val loginCallback = LoginCallback(client, activity)
    client.newCall(loginRequest).enqueue(loginCallback)
}

private fun getDocFromUrl(client: OkHttpClient, url: String) : Document {
    val request = Request.Builder()
        .url(url)
        .build()

    val response: Response
    runBlocking { response = client.newCall(request).execute() }
    return Jsoup.parse(response.body?.string() ?: "")
}

private fun getUrlWithWeek(gradeUrl: String, weekIndex: Int) : String {
    val year = GregorianCalendar().get(Calendar.YEAR)
    val tail = "&Year=$year&Week=$weekIndex&log=False"
    return gradeUrl + tail
}

private class LoginCallback(private val client: OkHttpClient, private val activity: MainActivity) : Callback {
    override fun onFailure(call: Call, e: IOException) {
        println("Login failure:")
        e.printStackTrace()
    }

    override fun onResponse(call: Call, response: Response) {
        if(!response.isSuccessful) {
            println("Login request failed: ${response.code}")
            return
        }

        // Check if user connected to their private office
        val isInPrivateOffice = response.request.url.toString() == "https://elschool.ru/users/privateoffice"

        // Notify activity about failed login so that error message could popup
        activity.hasLoginFailed.value = !isInPrivateOffice

        if(!isInPrivateOffice) {
            println("Couldn't login! Probably incorrect login and/or password")
            return
        }

        // Get correct url based on grade
        val diariesUrl = "https://elschool.ru/users/diaries"

        val diariesRequest: Request = Request.Builder()
            .url(diariesUrl)
            .build()

        val diariesResp: Response
        runBlocking { diariesResp = client.newCall(diariesRequest).execute() }

        val gradeUrl = GetGradeUrl().getUrl(
            diariesResp.request.url.toString(),
            Jsoup.parse(diariesResp.body?.string() ?: ""),
            "10А" )

        // Get full page (in case of holidays we might need to check for previous weeks)
        val fullPageWeekIndex = GetFullPage(client, gradeUrl).getFullPageWeekIndex()

        if(fullPageWeekIndex == null) {
            println("FullPageWeekIndex has not been found (GetFullPage.getFullPageWeekIndex() returned null)")
            return
        }

        // Get the schedule
        val schedule = GetSchedule(client, gradeUrl, fullPageWeekIndex).getSchedule()
        activity.schedule = schedule
    }
}

private class GetFullPage(
    private val client: OkHttpClient,
    private val gradeUrl: String
) {
    // Does schedule on this page have all lessons from monday to friday?
    private fun isPageFull(doc: Document) : Boolean {
        val diariesDiv = doc.find { e -> e.nameIs("div") && e.className() == "diaries" }
        val diaryNoLessonCount = diariesDiv?.count { e -> e.className() == "diary__nolesson" } ?: 0

        //If all days doesn't have any lessons (have diary__nolesson element), page is considered not full
        return diaryNoLessonCount != 6
    }

    private fun checkWeek(weekIndex: Int) : Int {
        if(weekIndex <= 2)
            return -1

        val weekUrl = getUrlWithWeek(gradeUrl, weekIndex)

        val weekRequest: Request = Request.Builder()
            .url(weekUrl)
            .build()

        val weekResp: Response
        runBlocking { weekResp = client.newCall(weekRequest).execute() }

        if(!isPageFull(Jsoup.parse(weekResp.body?.string() ?: ""))) {
            println("Week $weekUrl is not full!")
            return checkWeek(weekIndex - 1)
        }

        println("Week $weekUrl is full!")
        return weekIndex
    }

    fun getFullPageWeekIndex() : Int? {
        val thisWeekIndex = GregorianCalendar().get(Calendar.WEEK_OF_YEAR)

        println("Starting to check which week is full starting from week index $thisWeekIndex")
        val fullPageWeekIndex = checkWeek(thisWeekIndex)

        if(fullPageWeekIndex == -1)
            return null

        return fullPageWeekIndex
    }
}

private class GetGradeUrl {
    private fun getDepartmentId(doc: Document, grade: String) : String? {
        val gradeButton = doc.find {
                e -> e.className() == "dropdown-item" && e.nameIs("a") && e.text().trim() == grade
        }

        return gradeButton?.attribute("model-department-id")?.value
    }

    fun getUrl(originalUrl: String, doc: Document, grade: String) : String {
        val pattern = "departmentId=.*&"
        val departmentId = getDepartmentId(doc, grade)
        return Regex(pattern)
            .replace(originalUrl, "departmentId=$departmentId&")
    }
}

private class GetSchedule(
    private val client: OkHttpClient,
    private val gradeUrl: String,
    private val startWeekIndex: Int) {

    fun getSchedule() : Schedule {
        // Get indexes for even and uneven weeks
        val weekIndexEven: Int
        val weekIndexUneven: Int

        if(startWeekIndex % 2 == 0) {
            weekIndexEven = startWeekIndex
            weekIndexUneven = startWeekIndex - 1
        }
        else {
            weekIndexEven = startWeekIndex - 1
            weekIndexUneven = startWeekIndex
        }

        // Getting urls and making a request for each url
        val scheduleUrlEven = getUrlWithWeek(gradeUrl, weekIndexEven)
        val scheduleUrlUneven = getUrlWithWeek(gradeUrl, weekIndexUneven)

        println("ScheduleUrlEven: $scheduleUrlEven")
        println("ScheduleUrlUneven: $scheduleUrlUneven")

        val scheduleRequestEvenWeek = Request.Builder()
            .url(scheduleUrlEven)
            .build()

        val scheduleRequestUnevenWeek = Request.Builder()
            .url(scheduleUrlUneven)
            .build()

        var responseWeekEven: Response
        var responseWeekUneven: Response

        // Get response from both urls
        runBlocking {
            responseWeekEven = client.newCall(scheduleRequestEvenWeek).execute()
            responseWeekUneven = client.newCall(scheduleRequestUnevenWeek).execute()
        }

        println("Is even response successful: ${responseWeekEven.isSuccessful}")
        println("Is uneven response successful: ${responseWeekUneven.isSuccessful}")

        // Convert each html to week and and create a new schedule from these weeks
        fun getWeek(response: Response) : Array<Day> {
            val doc = Jsoup.parse(response.body?.string() ?: "")
            return convertToWeek(doc)
        }

        val weekEven = getWeek(responseWeekEven)
        val weekUneven = getWeek(responseWeekUneven)
        return Schedule(weekEven, weekUneven)
    }

    // Getting stuff from website and converting it to Array<Day>
    private fun convertToWeek(doc: Document) : Array<Day> {
        // Get lesson names
        val diariesDiv = doc.find { e -> e.nameIs("div") && e.className() == "diaries" }
        val lessonElements = diariesDiv?.filter { e -> e.className() == "diary__lesson" }
        val lessons = mutableListOf<Lesson>()

        lessonElements?.forEach { lessonElement ->
            val textDiv = lessonElement.find { e -> e.nameIs("div") && e.className() == "flex-grow-1" }
            val timeDiv = lessonElement.find { e -> e.nameIs("div") && e.className() == "diary__discipline__time" }

            if(textDiv != null && timeDiv != null) {
                val time = timeDiv.text().split("-")[0].trim()
                var subject = textDiv.text().split(".")[1].trim()
                val num = textDiv.text().split(".")[0]

                //Remove stuff after semicolon
                //We don't need stuff like "Информатика: Практическая работа"
                //Or "Физика: Лабораторная работа"
                if(subject.contains(':')) {
                    println("Before: $subject")
                    subject = subject.split(":")[0]
                    println("After: $subject")
                }

                //Leave only the first word
                //For example "Алгебра и начала математического анализа"
                //Could be shorten to just "Алгебра"
                //TODO: Apparently there is a bunch of subjects that couldn't be shorten like that like "Физическая культура"
                //subject = subject.split(" ")[0]

                // Add number to lesson's subject cause we need the number in the beginning to split lessons into days
                val resultSubject = "$num. $subject"

                lessons.add(
                    Lesson(
                        subject = resultSubject,
                        startTime = time,
                        cabinet = 0,
                    )
                )
            }
        }

        //Split lesson names into days
        val week = Array<MutableList<Lesson>>(DAY_COUNT) { mutableListOf() }

        val excludeSubjects = arrayOf(
            "Футбол для всех",
            "Индивидуальный проект",
            "Семьеведение (внеурочная деятельность)",
            "Основы финансовой грамотности",
            "Россия - Мои горизонты"
        )

        var dayIndex = 0
        var prevLessonNum = -1

        for(lesson in lessons) {
            val data = lesson.subject.split(".")
            val subject = data[1].trim() // Remove number in the beginning

            if(subject in excludeSubjects)
                continue

            val lessonNum = data[0].toInt()

            if(prevLessonNum > lessonNum)
                dayIndex += 1

            prevLessonNum = lessonNum

            if(dayIndex < week.size) {
                val day = week[dayIndex]
                day.add(Lesson(
                    subject = subject, // Subject without number at the beginning
                    startTime = lesson.startTime,
                    cabinet = lesson.cabinet)
                )
            }
        }

        return week.map {
            val resultLessons = arrayOfNulls<Lesson>(LESSON_COUNT)

            for(i in it.indices) {
                if(i !in resultLessons.indices)
                    throw Exception("Website's days appear to have more than 8 lessons!")

                resultLessons[i] = it[i]
            }

            Day(resultLessons)
        }.toTypedArray()
    }
}