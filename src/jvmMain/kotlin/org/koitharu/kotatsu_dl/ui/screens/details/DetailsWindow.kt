package org.koitharu.kotatsu_dl.ui.screens.details

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu_dl.logic.downloader.LocalDownloadManager
import org.koitharu.kotatsu_dl.ui.LocalResources
import org.koitharu.kotatsu_dl.ui.MangaCover
import org.koitharu.kotatsu_dl.ui.screens.Window
import org.koitharu.kotatsu_dl.ui.screens.WindowManager
import org.koitharu.kotatsu_dl.util.ParsersFactory

class DetailsWindow(
	private val state: WindowState,
	private val initialManga: Manga,
	private val wm: WindowManager,
) : Window {

	@OptIn(ExperimentalLayoutApi::class)
	@Composable
	override operator fun invoke() = Window(
		state = state,
		title = initialManga.title,
		onCloseRequest = { wm.close(this) },
		icon = painterResource("icon4xs.png"),
		resizable = true,
	) {
		var manga by remember { mutableStateOf(initialManga) }
		LaunchedEffect(initialManga) {
			manga = withContext(Dispatchers.Default) {
				ParsersFactory.createParser(initialManga.source).getDetails(initialManga)
			}
		}

		Row(
			modifier = Modifier.background(MaterialTheme.colorScheme.background)
		) {
			Column(
				modifier = Modifier
					.widthIn(min = 200.dp, max = 600.dp)
					.padding(8.dp)
					.fillMaxWidth(0.4f)
					.verticalScroll(rememberScrollState()),
			) {
				Surface(
					shape = RoundedCornerShape(4.dp),
					modifier = Modifier.fillMaxWidth().aspectRatio(13f / 18f),
				) {
					MangaCover(manga)
				}
				if (manga.tags.isNotEmpty()) {
					Spacer(Modifier.height(12.dp))
					FlowRow(
						horizontalArrangement = Arrangement.spacedBy(4.dp),
						verticalArrangement = Arrangement.spacedBy(4.dp),
						modifier = Modifier.fillMaxWidth(),
					) {
						for (tag in manga.tags) {
							SuggestionChip(
								onClick = {},
								label = { Text(tag.title) },
							)
						}
					}
				}
				if (!manga.description.isNullOrBlank()) {
					Spacer(Modifier.height(12.dp))
					Text(manga.description.orEmpty())
				}
			}
			Column(
				modifier = Modifier.fillMaxWidth(),
			) {
				Text(
					text = manga.title,
					style = MaterialTheme.typography.titleMedium,
				)
				manga.altTitle?.let { altTitle ->
					Spacer(Modifier.height(4.dp))
					Text(
						text = altTitle,
						style = MaterialTheme.typography.titleSmall,
					)
				}
				Spacer(Modifier.height(12.dp))
				val branches = remember(manga.chapters.orEmpty()) {
					manga.chapters.orEmpty().mapToSet { it.branch }
				}
				var branch by remember(branches) { mutableStateOf(branches.firstOrNull()) }
				val checkedChapters = remember(branch) {
					mutableStateMapOf<MangaChapter, Boolean>()
				}
				if (branches.size > 1) {
					val tabIndex = remember(branches, branch) { branches.indexOf(branch).coerceIn(branches.indices) }
					ScrollableTabRow(
						modifier = Modifier.fillMaxWidth(),
						selectedTabIndex = tabIndex,
					) {
						branches.forEachIndexed { index, b ->
							Tab(
								text = { Text(b ?: "Unknown") },
								selected = tabIndex == index,
								onClick = { branch = b },
							)
						}
					}
				}
				Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
					val listState = rememberLazyListState()
					val chapters = remember(manga.chapters, branch) { manga.getChapters(branch).orEmpty() }
					LazyColumn(
						modifier = Modifier.padding(4.dp),
						state = listState,
					) {
						items(chapters) { chapter ->
							Row(
								verticalAlignment = Alignment.CenterVertically,
							) {
								Checkbox(
									checked = checkedChapters[chapter] == true,
									onCheckedChange = { value -> checkedChapters[chapter] = value },
								)
								Spacer(Modifier.width(4.dp))
								Text(text = chapter.name)
							}
						}
					}
					VerticalScrollbar(
						modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).padding(vertical = 2.dp),
						adapter = rememberScrollbarAdapter(listState),
					)
				}
				Divider(
					modifier = Modifier
						.fillMaxWidth(),
				)
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp),
					horizontalArrangement = Arrangement.End,
				) {
					val dm = LocalDownloadManager.current
					Button(
						onClick = {
							val chaptersIds = checkedChapters.mapNotNullTo(HashSet()) { (k, v) ->
								if (v) k.id else null
							}
							if (dm.startDownload(window, manga, chaptersIds)) {
								wm.showDownloadsWindow()
							}
						},
						enabled = checkedChapters.any { it.value },
					) {
						Text(LocalResources.current.string("download"))
					}
				}
			}
		}
	}
}