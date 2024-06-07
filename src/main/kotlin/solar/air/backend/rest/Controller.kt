package solar.air.backend.rest

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
import java.io.File
import java.net.URLDecoder


@RestController
@RequestMapping("/solar")
 public class Controller {


    @GetMapping("data")
    fun data(
        @RequestParam("url") url: String,
        @RequestParam("cookie") cookie: String
    ): ResponseEntity<String> {

        val restTemplate = RestTemplate()

        val defaultUriBuilderFactory = DefaultUriBuilderFactory()
        defaultUriBuilderFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        restTemplate.uriTemplateHandler = defaultUriBuilderFactory

        val path = URLDecoder.decode(url,Charsets.UTF_8)



        val headers = HttpHeaders().apply {
            add(HttpHeaders.COOKIE,  URLDecoder.decode(cookie,Charsets.UTF_8))
            add(HttpHeaders.USER_AGENT, "insomnia/9.2.0")
            add(HttpHeaders.ACCEPT, "*/*")

        }


        val entity = HttpEntity<String>(headers)
        val response: ResponseEntity<String> = restTemplate.exchange(path, HttpMethod.GET, entity, String::class.java)

        return response
    }
}