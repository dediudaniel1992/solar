package solar.air.backend.rest

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriUtils
import java.net.URLDecoder
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@RestController
@RequestMapping("/solar")
class Controller {


    private var isLogged = false
    private var stationDn = ""
    private var cookie: String = ""
    private var restUrl = ""

    @GetMapping("data")
    fun data(
        @RequestParam("url") url: String,
        @RequestParam("user") user: String,
        @RequestParam("password") password: String
    ): ResponseEntity<String> {

        val restTemplate = RestTemplate()

        val defaultUriBuilderFactory = DefaultUriBuilderFactory()
        defaultUriBuilderFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        restTemplate.uriTemplateHandler = defaultUriBuilderFactory

        val path = URLDecoder.decode(url, Charsets.UTF_8)


        if (!isLogged) {
            login(path, user, password)
        }

        val value = this.cookie

        val headers = HttpHeaders().apply {
            add(HttpHeaders.COOKIE, value)
            add(HttpHeaders.USER_AGENT, "insomnia/9.2.0")
            add(HttpHeaders.ACCEPT, "*/*")

        }


        val entity = HttpEntity<String>(headers)
        val finalPath = getPath()
        val response: ResponseEntity<String> =
            restTemplate.exchange(finalPath, HttpMethod.GET, entity, String::class.java)

        if (response.body?.contains("login") == true) {
            this.isLogged = false
        }

        return response
    }

    private fun getTimeZone(currentDate: ZonedDateTime): Int {
        return if (currentDate.offset.totalSeconds == 0) {
            0
        } else {
            currentDate.offset.totalSeconds.div(3600)
        }
    }

    private fun getPath(): String {

        val restUrlSegments = this.restUrl.split("/")
        val normalizedUrl =
            restUrlSegments[0] + "//" + restUrlSegments[2] + "/rest/pvms/web/station/v1/overview/energy-balance"
        val currentDate = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDate = currentDate.format(formatter)
        val now = Instant.now().epochSecond * 1000

        val result = StringBuilder(normalizedUrl)

        return result
            .append("?stationDn=", UriUtils.encode(this.stationDn, Charsets.UTF_8))
            .append("&timeDim=", "2")
            .append(
                "&queryTime=",
                UriUtils.encode((currentDate.toInstant().epochSecond * 1000).toString(), Charsets.UTF_8)
            )
            .append("&timeZone=", UriUtils.encode(getTimeZone(currentDate).toString(), Charsets.UTF_8))
            .append("&timeZoneStr=", UriUtils.encode(currentDate.zone.id, Charsets.UTF_8))
            .append("&dateStr=", UriUtils.encode(formattedDate, Charsets.UTF_8))
            .append("&_=", UriUtils.encode(now.toString(), Charsets.UTF_8)).toString()


    }

    private fun login(path: String, user: String, password: String) {
        WebDriverManager.chromedriver().clearDriverCache().setup()
        WebDriverManager.chromedriver().clearResolutionCache().setup()
        WebDriverManager.chromedriver().setup()


        val options = ChromeOptions()
        options.addArguments("--lang=en")
        options.setExperimentalOption(
            "prefs", mapOf(
                "intl.accept_languages" to "en,en-US"
            )
        )

        // Initialize WebDriver
        val driver: WebDriver = ChromeDriver(options)

        // Open Google
        driver.get(path)

        try {
            val wait = WebDriverWait(/* driver = */ driver, /* timeout = */ Duration.ofSeconds(2))
            val acceptButton: WebElement =
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[.//div[text()='I agree']]")))
            acceptButton.click()
        } catch (e: Exception) {
            println("No popup appeared: ${e.message}")
        }


        // Find the search box by name and type a query
        val username = driver.findElement(By.id("username"))
        val value = driver.findElement(By.id("value"))
        username.sendKeys(user)
        value.sendKeys(password)
        driver.findElement(By.className("loginBtn")).click()

        // Wait for the results to load and display the title
        Thread.sleep(2000)
        this.cookie = driver.manage().cookies.joinToString(separator = "; ") { "${it.name}=${it.value}" }
        this.isLogged = true
        Thread.sleep(15000)
        this.restUrl = driver.currentUrl
        getStationDn(driver.currentUrl)
        // Close the browser
        driver.quit()

    }


    private fun getStationDn(currentUrl: String) {

        val regexPattern = "station/([^/]+)".toRegex()
        val matchResult = regexPattern.find(currentUrl)
        this.stationDn = matchResult?.groupValues?.get(1).toString()

    }


}

