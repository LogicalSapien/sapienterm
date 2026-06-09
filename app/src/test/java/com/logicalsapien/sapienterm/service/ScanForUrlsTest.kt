/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logicalsapien.sapienterm.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.regex.Pattern

/**
 * Validates URL extraction logic aligned with [TerminalBridge] (private [Pattern] + deduplication
 * via [Set]). Regex is duplicated here so tests stay free of Robolectric and bridge dependencies.
 */
class ScanForUrlsTest {

    private val urlPattern: Pattern = run {
        val scheme = "[A-Za-z][-+.0-9A-Za-z]*"
        val unreserved = "[-._~0-9A-Za-z]"
        val pctEncoded = "%[0-9A-Fa-f]{2}"
        val subDelims = "[!\$&'()*+,;:=]"
        val userinfo = "(?:$unreserved|$pctEncoded|$subDelims|:)*"
        val h16 = "[0-9A-Fa-f]{1,4}"
        val decOctet = "(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])"
        val ipv4address = "$decOctet\\.$decOctet\\.$decOctet\\.$decOctet"
        val ls32 = "(?:$h16:$h16|$ipv4address)"
        val ipv6address = "(?:(?:$h16){6}$ls32)"
        val ipvfuture = "v[0-9A-Fa-f]+.(?:$unreserved|$subDelims|:)+"
        val ipLiteral = "\\[(?:$ipv6address|$ipvfuture)\\]"
        val regName = "(?:$unreserved|$pctEncoded|$subDelims)*"
        val host = "(?:$ipLiteral|$ipv4address|$regName)"
        val port = "[0-9]*"
        val authority = "(?:$userinfo@)?$host(?::$port)?"
        val pchar = "(?:$unreserved|$pctEncoded|$subDelims|@)"
        val segment = "$pchar*"
        val pathAbempty = "(?:/$segment)*"
        val segmentNz = "$pchar+"
        val pathAbsolute = "/(?:$segmentNz(?:/$segment)*)?"
        val pathRootless = "$segmentNz(?:/$segment)*"
        val hierPart = "(?://$authority$pathAbempty|$pathAbsolute|$pathRootless)"
        val query = "(?:$pchar|/|\\?)*"
        val fragment = "(?:$pchar|/|\\?)*"
        val uriRegex = "$scheme:$hierPart(?:$query)?(?:#$fragment)?"
        Pattern.compile(uriRegex)
    }

    /** Mirrors [TerminalBridge.scanForURLs] matching and deduplication. */
    private fun findUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            matcher.group()?.let { urls.add(it) }
        }
        return urls.toSet().toList()
    }

    @Test
    fun findsHttpUrlInText() {
        val text = "Visit http://example.com today"
        assertThat(findUrls(text)).contains("http://example.com")
    }

    @Test
    fun findsHttpsUrlInText() {
        assertThat(findUrls("See https://secure.example.org/path")).contains("https://secure.example.org/path")
    }

    @Test
    fun findsUrlWithPort() {
        val url = "https://api.example.com:8443"
        assertThat(findUrls("curl $url")).containsExactlyInAnyOrder(url)
    }

    @Test
    fun findsUrlWithPathAndQueryString() {
        val url = "http://foo.bar/baz/qux?x=1&y=two"
        assertThat(findUrls("link: $url trailing")).containsExactlyInAnyOrder(url)
    }

    @Test
    fun returnsEmptyWhenTextHasNoUrls() {
        assertThat(findUrls("plain prose only")).isEmpty()
        assertThat(findUrls("not.a valid url without scheme")).isEmpty()
    }

    @Test
    fun deduplicatesRepeatedUrls() {
        val url = "https://example.com/a"
        val text = "one $url two $url"
        assertThat(findUrls(text)).hasSize(1)
        assertThat(findUrls(text)).contains(url)
    }
}
