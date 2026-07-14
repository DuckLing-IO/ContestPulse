package io.duckling.contestpulse.navigation

import android.net.Uri
import io.duckling.contestpulse.feature.contestdetail.ContestDetailViewModel

object ContestDetailDestination {
    const val route = "contest/{${ContestDetailViewModel.CONTEST_ID_ARGUMENT}}"
    const val deepLinkPattern = "contestpulse://contest/{${ContestDetailViewModel.CONTEST_ID_ARGUMENT}}"

    fun createRoute(contestId: String): String = "contest/${Uri.encode(contestId)}"

    fun deepLinkUri(contestId: String): Uri =
        Uri.parse("contestpulse://contest/${Uri.encode(contestId)}")
}
