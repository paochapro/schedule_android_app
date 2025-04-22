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

const val LOGIN_FAIL_STATUS_REQUEST_FAIL = "REQUEST_ISNT_SUCCESSFUL"
const val LOGIN_FAIL_STATUS_RESPONSE_FAIL = "RESPONSE_ISNT_SUCCESSFUL"
const val LOGIN_FAIL_STATUS_INCORRECT_LOGIN_OR_PASSWORD = "INCORRECT_LOGIN_OR_PASSWORD"
const val LOGIN_FAIL_STATUS_EMPTY_SCHEDULE = "EMPTY_SCHEDULE"
const val LOGIN_SUCCESS_STATUS = "SUCCESS"
const val LOGIN_STATUS_WAIT = "WAIT"
const val LOGIN_STATUS_NONE = ""

fun readWebsiteAndStoreInSchedule(activity: MainActivity, login: String, password: String, grade: String) {
    val builder: OkHttpClient.Builder = OkHttpClient.Builder()
    val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(activity))
    builder.cookieJar(cookieJar)
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

    val loginCallback = LoginCallback(client, activity, grade)
    client.newCall(loginRequest).enqueue(loginCallback)
}

private fun docFromUrl(client: OkHttpClient, url: String) : Document {
    val request = Request.Builder()
        .url(url)
        .build()

    val response: Response
    runBlocking { response = client.newCall(request).execute() }
    return Jsoup.parse(response.body?.string() ?: "")
}

private fun getGradeUrlWithWeek(gradeUrl: String, weekIndex: Int) : String {
    val year = GregorianCalendar().get(Calendar.YEAR)
    val tail = "&Year=$year&Week=$weekIndex&log=False"
    return gradeUrl + tail
}

// Does schedule on this page have all lessons from monday to friday?
private fun isPageFull(doc: Document) : Boolean {
    val diariesDiv = doc.find { e -> e.nameIs("div") && e.className() == "diaries" }
    val diaryNoLessonCount = diariesDiv?.count { e -> e.className() == "diary__nolesson" } ?: 0

    //If all days doesn't have any lessons (have diary__nolesson element), page is considered not full
    return diaryNoLessonCount != 6
}

private class LoginCallback(
    private val client: OkHttpClient,
    private val activity: MainActivity,
    private val grade: String)
    : Callback {
    override fun onFailure(call: Call, e: IOException) {
        activity.hasLoginFailedMsg.value = LOGIN_FAIL_STATUS_REQUEST_FAIL
        println("Login failure:")
        e.printStackTrace()
    }

    override fun onResponse(call: Call, response: Response) {
        if(!response.isSuccessful) {
            activity.hasLoginFailedMsg.value = LOGIN_FAIL_STATUS_RESPONSE_FAIL
            println("Login request failed: ${response.code}")
            return
        }

        // Check if user connected to their private office
        val isInPrivateOffice = response.request.url.toString() == "https://elschool.ru/users/privateoffice"

        // Notify activity about failed login so that error message could popup
        activity.hasLoginFailedMsg.value = LOGIN_STATUS_WAIT

        if(!isInPrivateOffice) {
            activity.hasLoginFailedMsg.value = LOGIN_FAIL_STATUS_INCORRECT_LOGIN_OR_PASSWORD
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
            grade)


        //Get startWeekIndex
        val getFullPage = GetFullPage(client, gradeUrl)

        var startWeekIndex = GregorianCalendar().get(Calendar.WEEK_OF_YEAR)

        val inHolidays = !getFullPage.isWeekFull(startWeekIndex)

        //If today's week is not holidays, that means that either previous or next week are not holidays
        if(!inHolidays) {
            //If previous week is holidays
            //That means the next week is full
            if(!getFullPage.isWeekFull(startWeekIndex - 1)) {
                startWeekIndex += 1
            }
        }
        //If today's week is holidays
        //That means that we should find the first full week going backwards (going from week 15 to 14 to 13)
        //And the previous week of that first week is almost guaranteed to be full
        else {
            //We subtract, cause we know that the current week is holidays
            print("Current week $startWeekIndex is not full. ")

            startWeekIndex -= 1

            println("Starting to check which week is full starting from week index $startWeekIndex")
            val fullPageWeekIndex = getFullPage.getFullWeek(startWeekIndex)

            if(fullPageWeekIndex == -1) {
                println("FullPageWeekIndex has not been found (GetFullPage.getFullWeek() returned null)")
                activity.hasLoginFailedMsg.value = LOGIN_FAIL_STATUS_EMPTY_SCHEDULE
                return
            }

            startWeekIndex = fullPageWeekIndex
        }

        // In the end startWeekIndex is full week
        // And the previous week of startWeekIndex is full too
        // So the last thing there is to do
        // Is to understand which one is even and which one is uneven
        println("StartWeekIndex: $startWeekIndex")

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

        // Get the schedule
        val schedule = GetSchedule(client, gradeUrl,
            weekIndexEven = weekIndexEven,
            weekIndexUneven = weekIndexUneven)
            .getSchedule()

        activity.schedule = schedule
        activity.onScheduleUpdate()
        println("Successfully imported schedule")
        activity.hasLoginFailedMsg.value = LOGIN_SUCCESS_STATUS

        val cookieJar = (client.cookieJar as PersistentCookieJar)
        cookieJar.clear()
        cookieJar.clearSession()
        println("Successfully cleared cookies")
    }
}

private class GetFullPage(
    private val client: OkHttpClient,
    private val gradeUrl: String
) {
    fun isWeekFull(weekIndex: Int) : Boolean {
        val weekUrl = getGradeUrlWithWeek(gradeUrl, weekIndex)
        return isPageFull(docFromUrl(client, weekUrl))
    }

    fun getFullWeek(weekIndex: Int) : Int {
        if(weekIndex <= 2)
            return -1

        val isWeekFull = isWeekFull(weekIndex)

        if(!isWeekFull) {
            println("Week $weekIndex is not full!")
            return getFullWeek(weekIndex - 1)
        }

        println("Week $weekIndex is full!")
        return weekIndex
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
    private val weekIndexEven: Int,
    private val weekIndexUneven: Int) {

    fun getSchedule() : Schedule {
        // Getting urls and making a request for each url
        val scheduleUrlEven = getGradeUrlWithWeek(gradeUrl, weekIndexEven)
        val scheduleUrlUneven = getGradeUrlWithWeek(gradeUrl, weekIndexUneven)

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
                var time = timeDiv.text().split("-")[0].trim()
                var subject = textDiv.text().split(".")[1].trim()
                val num = textDiv.text().split(".")[0]

                //Convert 08:00 to 8:00
                val timeHours = time.split(":")[0]
                val timeMinutes = time.split(":")[1]
                if(timeHours.length == 2 && timeHours[0] == '0') {
                    time = "${timeHours[1]}:$timeMinutes"
                }

                //Remove stuff after semicolon
                //We don't need stuff like "Информатика: Практическая работа"
                //Or "Физика: Лабораторная работа"
                if(subject.contains(':')) {
                    subject = subject.split(":")[0]
                }

                //Remap long subjects to short version
                val remap = mapOf(
                    "Алгебра и начала математического анализа" to "Алгебра",
                    "Основы безопасности и защиты Родины" to "Обзр",
                    "Иностранный язык (английский)" to "Английский",
                    "Вероятность и статистика" to "Вероятность"
                )

                if(remap.containsKey(subject)) {
                    subject = remap.getValue(subject)
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
                        cabinet = -1,
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
            "Россия - Мои горизонты",
            "Классный час"
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

        return week.mapIndexed { dIndex, day ->
            val resultLessons = arrayOfNulls<Lesson>(LESSON_COUNT)

            for(i in day.indices) {
                if(i !in resultLessons.indices)
                    throw Exception("Website's days appear to have more than 8 lessons!")

                resultLessons[i] = day[i]
            }

            //Just making an assumption that monday has 35 mins lesson length
            val lessonLength = if(dIndex == 0) MONDAY_LESSON_TIME_MINS else DEFAULT_LESSON_TIME_MINS
            Day(resultLessons, lessonLength)
        }.toTypedArray()
    }
}