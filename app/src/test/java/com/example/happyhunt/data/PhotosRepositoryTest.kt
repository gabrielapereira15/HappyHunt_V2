package com.example.happyhunt.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotosRepositoryTest {
    private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()

    @Test
    fun `the photo named on a Wikidata item`() {
        val claims = """{"claims":{"P18":[{"mainsnak":{"datavalue":{"value":"Royal Ontario Museum-Michael Lee-Chin Crystal.jpg","type":"string"}}}]}}"""
        assertEquals("Royal Ontario Museum-Michael Lee-Chin Crystal.jpg", PhotosRepository.fileFromClaims(claims))
        assertNull(PhotosRepository.fileFromClaims("""{"claims":{}}"""))
        assertNull(PhotosRepository.fileFromClaims("not json"))
    }

    @Test
    fun `a photo comes with who to credit for it`() {
        val photo = PhotosRepository.photoFromImageInfo(fixture("commons-imageinfo.json"))!!
        assertEquals("Staka", photo.author)
        assertEquals("CC BY-SA 4.0", photo.license)
        assertEquals("https://commons.wikimedia.org/wiki/File:Royal_Ontario_Museum-Michael_Lee-Chin_Crystal.jpg", photo.creditUrl)
        assertEquals(true, photo.url.contains("960px-Royal_Ontario_Museum"))
    }

    @Test
    fun `without the photo's page, the file is still found by name`() {
        assertNull(PhotosRepository.photoFromImageInfo("""{"query":{"pages":[{"missing":true}]}}"""))
        val photo = PhotosRepository.photoFor("Royal Ontario Museum.jpg")
        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/Royal_Ontario_Museum.jpg?width=960", photo.url)
        assertNull(photo.author)
    }
}
