// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import java.util.Locale
import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class BelgeselX : MainAPI() {
    override var mainUrl              = "https://belgeselx.com"
    override var name                 = "BelgeselX"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/konu/turk-tarihi-belgeselleri&page=" to "Türk Tarihi Belgeselleri",
        "${mainUrl}/konu/tarih-belgeselleri&page="		 to "Tarih Belgeselleri",
        "${mainUrl}/konu/seyehat-belgeselleri&page="	 to "Seyehat Belgeselleri",
        "${mainUrl}/konu/seri-belgeseller&page="		 to "Seri Belgeseller",
        "${mainUrl}/konu/savas-belgeselleri&page="		 to "Savaş Belgeselleri",
        "${mainUrl}/konu/sanat-belgeselleri&page="		 to "Sanat Belgeselleri",
        "${mainUrl}/konu/psikoloji-belgeselleri&page="	 to "Psikoloji Belgeselleri",
        "${mainUrl}/konu/polisiye-belgeselleri&page="	 to "Polisiye Belgeselleri",
        "${mainUrl}/konu/otomobil-belgeselleri&page="	 to "Otomobil Belgeselleri",
        "${mainUrl}/konu/nazi-belgeselleri&page="		 to "Nazi Belgeselleri",
        "${mainUrl}/konu/muhendislik-belgeselleri&page=" to "Mühendislik Belgeselleri",
        "${mainUrl}/konu/kultur-din-belgeselleri&page="	 to "Kültür Din Belgeselleri",
        "${mainUrl}/konu/kozmik-belgeseller&page="		 to "Kozmik Belgeseller",
        "${mainUrl}/konu/hayvan-belgeselleri&page="		 to "Hayvan Belgeselleri",
        "${mainUrl}/konu/eski-tarih-belgeselleri&page="	 to "Eski Tarih Belgeselleri",
        "${mainUrl}/konu/egitim-belgeselleri&page="		 to "Eğitim Belgeselleri",
        "${mainUrl}/konu/dunya-belgeselleri&page="		 to "Dünya Belgeselleri",
        "${mainUrl}/konu/doga-belgeselleri&page="		 to "Doğa Belgeselleri",
        "${mainUrl}/konu/cizgi-film&page="				 to "Çizgi Film",
        "${mainUrl}/konu/bilim-belgeselleri&page="		 to "Bilim Belgeselleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.gen-movie-contain").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun String.toTitleCase(): String {
        val locale = Locale("tr", "TR")
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text()?.trim()?.toTitleCase() ?: return null
        val href      = fixUrlNull(this.selectFirst("h3 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return mutableListOf<SearchResponse>()
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h2.gen-title")?.text()?.trim()?.toTitleCase() ?: return null
        val poster      = fixUrlNull(document.selectFirst("div.gen-tv-show-top img")?.attr("src")) ?: return null
        val description = document.selectFirst("div.gen-single-tv-show-info p")?.text()?.trim()
        val tags        = document.select("div.gen-socail-share a[href*='belgeselkanali']").map { it.attr("href").split("/").last().replace("-", " ").toTitleCase() }

        val episodes = document.select("div.gen-movie-contain").mapNotNull {
            val ep_name     = it.selectFirst("div.gen-movie-info h3 a")?.text()?.trim() ?: return@mapNotNull null
            val ep_href     = fixUrlNull(it.selectFirst("div.gen-movie-info h3 a")?.attr("href")) ?: return@mapNotNull null

            val season_name = it.selectFirst("div.gen-single-meta-holder ul li")?.text()?.trim() ?: ""
            val ep_episode  = Regex("""Bölüm (\d+)""").find(season_name)?.groupValues?.get(1)?.toIntOrNull()
            val ep_season   = Regex("""Sezon (\d+)""").find(season_name)?.groupValues?.get(1)?.toIntOrNull() ?: 1

            Episode(
                data    = ep_href,
                name    = ep_name,
                season  = ep_season,
                episode = ep_episode
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot      = description
            this.tags      = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("BLX", "data » ${data}")
        val source = app.get(data)

        Regex("""<iframe\s+[^>]*src=\\\"([^\\\"']+)\\\"""").findAll(source.text).forEach { alternatif ->
            val alternatif_url = alternatif.groupValues[1]
            Log.d("BLX", "alternatif_url » ${alternatif_url}")

            if (alternatif_url.contains("new4.php")) {
                val new4_source = app.get(alternatif_url).text

                Regex("""file:\"([^\"]+)\", label: \"([^\"]+)""").findAll(new4_source).forEach {
                    val video_url = it.groupValues[1]
                    val quality   = it.groupValues[2].replace("FULL",   "1080p")

                    callback.invoke(
                        ExtractorLink(
                            source  = this.name,
                            name    = this.name,
                            url     = video_url,
                            referer = data,
                            quality = getQualityFromName(quality),
                            type    = INFER_TYPE
                        )
                    )
                }
            }

            // loadExtractor(alternatif_url, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}