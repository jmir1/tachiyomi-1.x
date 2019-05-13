package tachiyomi.source

import tachiyomi.source.model.FilterList
import tachiyomi.source.model.Listing
import tachiyomi.source.model.MangasPageInfo

interface CatalogSource : Source {

  override val lang: String

  suspend fun fetchMangaList(sort: Listing?, page: Int): MangasPageInfo

  suspend fun fetchMangaList(filters: FilterList, page: Int): MangasPageInfo

  fun getListings(): List<Listing>

  fun getFilters(): FilterList

}
