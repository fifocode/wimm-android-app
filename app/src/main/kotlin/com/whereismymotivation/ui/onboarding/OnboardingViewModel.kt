package com.whereismymotivation.ui.onboarding

import androidx.compose.runtime.mutableStateListOf
import com.whereismymotivation.R
import com.whereismymotivation.data.model.Mentor
import com.whereismymotivation.data.model.Topic
import com.whereismymotivation.data.repository.MentorRepository
import com.whereismymotivation.data.repository.SubscriptionRepository
import com.whereismymotivation.data.repository.TopicRepository
import com.whereismymotivation.data.repository.UserRepository
import com.whereismymotivation.ui.base.BaseViewModel
import com.whereismymotivation.ui.common.progress.Loader
import com.whereismymotivation.ui.common.snackbar.Message
import com.whereismymotivation.ui.common.snackbar.Messenger
import com.whereismymotivation.ui.navigation.Destination
import com.whereismymotivation.ui.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    loader: Loader,
    private val messenger: Messenger,
    private val navigator: Navigator,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val mentorRepository: MentorRepository,
    private val topicRepository: TopicRepository
) : BaseViewModel(loader, messenger, navigator) {

    companion object {
        const val TAG = "OnboardingViewModel"
    }

    init {
        checkAndLoadSuggestions()
    }

    val mentors = mutableStateListOf<Mentor>()
    val topics = mutableStateListOf<Topic>()

    private var loading = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun checkAndLoadSuggestions() {
        launchNetwork(silent = true, error = { loadSuggestions() }) {
            mentorRepository
                .fetchSubscriptionMentors()
                .flatMapLatest { mentors ->
                    if (mentors.isNotEmpty()) return@flatMapLatest flowOf(true)
                    else return@flatMapLatest topicRepository.fetchSubscriptionTopics()
                        .map { topics -> topics.isNotEmpty() }
                }
                .collect {
                    if (it) {
                        userRepository.markUserOnBoardingComplete()
                        navigator.navigateTo(Destination.Home.route, true)
                    } else {
                        loadSuggestions()
                    }
                }
        }
    }

    private fun loadSuggestions() {
        launchNetwork {
            combine(
                topicRepository
                    .fetchRecommendedTopics(1, 50),
                mentorRepository
                    .fetchRecommendedMentors(1, 50),
            ) { t, m ->
                topics.addAll(t)
                mentors.addAll(m)
            }.collect()
        }
    }

    fun topicSelect(topic: Topic) {
        val index = topics.indexOf((topic))
        if (index > -1) topics[index] = topic.copy(subscribed = true)
    }

    fun topicUnselect(topic: Topic) {
        val index = topics.indexOf((topic))
        if (index > -1) topics[index] = topic.copy(subscribed = false)

    }

    fun mentorSelect(mentor: Mentor) {
        val index = mentors.indexOf((mentor))
        if (index > -1) mentors[index] = mentor.copy(subscribed = true)
    }

    fun mentorUnselect(mentor: Mentor) {
        val index = mentors.indexOf((mentor))
        if (index > -1) mentors[index] = mentor.copy(subscribed = false)
    }

    fun complete() {
        if (loading) return
        loading = true

        val selectedMentors = mentors.filter { it.subscribed == true }
        val selectedTopic = topics.filter { it.subscribed == true }
        if (selectedTopic.isEmpty()) {
            return messenger.deliverRes(Message.warning(R.string.select_few_topics))
        }

        if (selectedMentors.isEmpty()) {
            return messenger.deliverRes(Message.warning(R.string.select_some_mentors))
        }

        launchNetwork(error = { loading = false }) {
            subscriptionRepository
                .subscribe(
                    selectedMentors,
                    selectedTopic
                )
                .collect {
                    messenger.deliver(Message.success(it))
                    userRepository.markUserOnBoardingComplete()
                    navigator.navigateTo(Destination.Home.route, true)
                    loading = false
                }
        }
    }
}