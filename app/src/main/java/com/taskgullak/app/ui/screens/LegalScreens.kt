package com.taskgullak.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.ui.components.CssGridOverlay
import com.taskgullak.app.ui.components.Floating3DOrb
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgLegalBlue100
import com.taskgullak.app.ui.theme.TgLegalBlue400
import com.taskgullak.app.ui.theme.TgLegalBlue500
import com.taskgullak.app.ui.theme.TgLegalBlue600
import com.taskgullak.app.ui.theme.TgLegalBlue700
import com.taskgullak.app.ui.theme.TgLegalGreen
import com.taskgullak.app.ui.theme.TgLegalGreenBorder
import com.taskgullak.app.ui.theme.TgLegalGreenSoft
import com.taskgullak.app.ui.theme.TgLegalMuted
import com.taskgullak.app.ui.theme.TgLegalOrange
import com.taskgullak.app.ui.theme.TgLegalOrangeBorder
import com.taskgullak.app.ui.theme.TgLegalOrangeSoft
import com.taskgullak.app.ui.theme.TgLegalText
import com.taskgullak.app.ui.theme.TgLegalTextSoft
import com.taskgullak.app.ui.theme.TgLegalYellowBorder
import com.taskgullak.app.ui.theme.TgLegalYellowSoft
import kotlinx.coroutines.launch

/**
 * Exact 100% UI replica of terms.html + terms.css
 */
@Composable
fun TermsScreen(
    fromSource: String = "login",
    onBack: () -> Unit,
    onSwitchToPrivacy: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val backButtonLabel = if (fromSource == "signup") "Back to Sign Up" else "Back to Login"

    val tocItems = listOf(
        "Acceptance of Terms" to 1,
        "Eligibility" to 2,
        "Account & Security" to 3,
        "Tasks & Participation" to 4,
        "Proof & Submissions" to 5,
        "Rewards & Payments" to 6,
        "Prohibited Activities" to 7,
        "Verification & Review" to 8,
        "Suspension & Termination" to 9,
        "Privacy" to 10,
        "Third-Party Services" to 11,
        "Changes to Terms" to 12,
        "Contact Us" to 13
    )

    LegalSkyBackground(testTag = "terms_screen") {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                // Start of Main Card Top Header + Intro + TOC
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clip(RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFBFE0F1).copy(alpha = 0.86f),
                            shape = RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LegalHeaderBlock(
                        badgeText = "LEGAL",
                        title = "Terms & Conditions",
                        subtitle = "Please read these terms carefully before using Task Gullak.",
                        metaRight = "User Agreement"
                    )

                    // INTRODUCTION (.terms-intro)
                    LegalIntroBox(
                        paragraphs = listOf(
                            "These Terms & Conditions explain the rules that apply when you create an account, use Task Gullak, complete tasks, submit proof, receive rewards, or use any other part of our platform.",
                            "By creating an account or using Task Gullak, you agree to follow these Terms. If you do not agree with them, please do not use the platform."
                        )
                    )

                    // TABLE OF CONTENTS (.terms-toc)
                    LegalTocGrid(
                        heading = "Contents",
                        items = tocItems,
                        onSelectSection = { sectionIndex ->
                            coroutineScope.launch {
                                listState.animateScrollToItem(sectionIndex)
                            }
                        }
                    )
                }
            }

            // SECTION 01
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "01",
                        title = "Acceptance of Terms",
                        paragraphs = listOf(
                            "By accessing or using Task Gullak, you confirm that you understand and agree to these Terms & Conditions.",
                            "These Terms apply to the Task Gullak website, user account, tasks, submissions, rewards, wallet features, verification systems and other services provided through the platform.",
                            "If you are using Task Gullak on behalf of another person or organization, you confirm that you have the authority to accept these Terms on their behalf."
                        )
                    )
                }
            }

            // SECTION 02
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "02",
                        title = "Eligibility",
                        paragraphs = listOf(
                            "You must meet the minimum age and other legal requirements applicable to use Task Gullak in your location.",
                            "You are responsible for making sure that your participation in tasks and receipt of rewards is lawful for you."
                        ),
                        bullets = listOf(
                            "You must provide truthful information.",
                            "You must not create an account using someone else's identity.",
                            "You must not use Task Gullak if doing so would violate applicable law.",
                            "You must follow the requirements shown for each task."
                        ),
                        warningBox = "Important" to "Task availability may depend on age, location, eligibility, verification, task requirements or other conditions."
                    )
                }
            }

            // SECTION 03
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "03",
                        title = "Account & Security",
                        paragraphs = listOf(
                            "Some Task Gullak features require an account. You are responsible for keeping your account information accurate and protecting your login credentials."
                        ),
                        bullets = listOf(
                            "Use your own email address or permitted authentication method.",
                            "Keep your password confidential.",
                            "Do not share your account with another person.",
                            "Notify us if you believe your account has been accessed without permission.",
                            "Do not attempt to access another user's account."
                        ),
                        closingParagraph = "Task Gullak may require email verification or additional checks before allowing certain account actions."
                    )
                }
            }

            // SECTION 04
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "04",
                        title = "Tasks & Participation",
                        paragraphs = listOf(
                            "Task Gullak may display different types of tasks, such as surveys, research activities, app testing, beta testing, feedback requests, bug reporting, data-related activities or other authorized user tasks.",
                            "Each task may have its own instructions, eligibility requirements, deadline, reward, proof requirements and verification criteria.",
                            "You should read the complete task instructions before accepting or starting a task."
                        ),
                        bullets = listOf(
                            "Complete the task according to its instructions.",
                            "Do not intentionally submit incomplete work.",
                            "Do not claim a task that you do not intend to complete.",
                            "Do not use unauthorized automation, bots or scripts.",
                            "Do not manipulate task results or verification."
                        )
                    )
                }
            }

            // SECTION 05
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "05",
                        title = "Proof & Submissions",
                        paragraphs = listOf(
                            "Some tasks require you to submit proof that you completed the requested activity.",
                            "Proof may include screenshots, photographs, written responses, recordings, links, task-specific information or other material requested in the task instructions.",
                            "You are responsible for ensuring that your submission is clear, relevant and follows the requirements of the specific task."
                        ),
                        bullets = listOf(
                            "Submit genuine proof of your own work.",
                            "Do not upload fake, edited or misleading evidence when genuine proof is required.",
                            "Do not submit the same proof for unrelated tasks unless the task specifically allows it.",
                            "Do not include unnecessary private information belonging to another person.",
                            "Follow all instructions regarding screenshot or image quality."
                        ),
                        infoBox = "Simple rule" to "If a task asks for proof, submit proof that clearly shows that you completed the required activity."
                    )
                }
            }

            // SECTION 06
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "06",
                        title = "Rewards & Payments",
                        paragraphs = listOf(
                            "Task Gullak may provide rewards for eligible tasks that are completed and properly verified according to the applicable task requirements."
                        ),
                        paymentWarning = "Payment is not automatic" to "Completing a task does not guarantee payment. Payment will be added only if the submitted task and proof are properly verified and approved according to the applicable task requirements.",
                        extraParagraphsAfterPaymentWarning = listOf(
                            "A task may be rejected when the submitted work does not satisfy the stated requirements, proof is insufficient, information is inaccurate, or there is evidence of misuse or manipulation.",
                            "Reward amounts, eligibility conditions and withdrawal requirements may vary between tasks and may be displayed before participation."
                        ),
                        bullets = listOf(
                            "Rewards are subject to successful verification.",
                            "A displayed reward is not a promise that the task will be approved.",
                            "Duplicate, fraudulent or invalid submissions may not receive rewards.",
                            "Withdrawal availability may depend on the applicable minimum balance and other conditions.",
                            "Payment or withdrawal processing may take reasonable processing time."
                        ),
                        warningBox = "Important" to "Never assume that simply claiming or submitting a task creates an unconditional right to payment."
                    )
                }
            }

            // SECTION 07
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "07",
                        title = "Prohibited Activities",
                        paragraphs = listOf(
                            "To keep Task Gullak fair and safe, you must not misuse the platform, tasks, verification systems or other users."
                        ),
                        bullets = listOf(
                            "Creating multiple accounts to gain unauthorized benefits.",
                            "Using fake or stolen identities.",
                            "Providing false or misleading information.",
                            "Submitting fabricated proof.",
                            "Manipulating screenshots or other evidence to falsely show task completion.",
                            "Using bots, unauthorized automation or scripts to abuse task systems.",
                            "Attempting to bypass security or verification.",
                            "Interfering with another user's account.",
                            "Exploiting technical bugs for unauthorized rewards or benefits.",
                            "Using Task Gullak for illegal activities."
                        ),
                        warningBox = "Zero tolerance" to "Serious fraud, abuse, account manipulation or attempts to defeat platform security may result in immediate restriction or termination."
                    )
                }
            }

            // SECTION 08
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "08",
                        title = "Verification & Review",
                        paragraphs = listOf(
                            "Task Gullak may review task submissions before approving rewards.",
                            "Verification may be performed using automated systems, manual review, task-specific checks, or a combination of these methods.",
                            "A submission may remain under review for a reasonable period depending on the task and verification requirements."
                        ),
                        bullets = listOf(
                            "Verification does not guarantee approval.",
                            "A submission can be rejected if it does not meet the task requirements.",
                            "Additional information or proof may sometimes be requested.",
                            "Repeated suspicious activity may trigger additional review."
                        ),
                        infoBox = "Why verification exists" to "Verification helps protect task providers, genuine users and the platform from inaccurate, duplicate or fraudulent submissions."
                    )
                }
            }

            // SECTION 09
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "09",
                        title = "Suspension & Termination",
                        paragraphs = listOf(
                            "Task Gullak may temporarily restrict, suspend or terminate an account when necessary to protect the platform, users or task providers.",
                            "This may happen when we reasonably believe that an account has:"
                        ),
                        bullets = listOf(
                            "Violated these Terms.",
                            "Participated in fraud or abuse.",
                            "Submitted repeatedly invalid information.",
                            "Attempted to manipulate rewards or verification.",
                            "Created a security or safety risk."
                        ),
                        closingParagraph = "Where appropriate and permitted, users may contact support regarding an account restriction or review.",
                        warningBox = "Account safety" to "Do not create another account to bypass an existing suspension or restriction."
                    )
                }
            }

            // SECTION 10
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "10",
                        title = "Privacy",
                        paragraphs = listOf(
                            "Your use of Task Gullak may involve the collection and processing of information required to provide our services.",
                            "Our Privacy Policy explains what information may be collected, why it may be used, how it may be protected and what choices may be available to you."
                        ),
                        infoBox = "Read the Privacy Policy" to "Please review the Task Gullak Privacy Policy before using the platform so that you understand how your information is handled."
                    )
                }
            }

            // SECTION 11
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "11",
                        title = "Third-Party Services",
                        paragraphs = listOf(
                            "Task Gullak may use third-party services to provide certain features or infrastructure.",
                            "These services may include authentication, hosting, storage, analytics, communication, payment processing or other technical services.",
                            "Some third-party services may have their own terms and privacy policies. You may be required to follow those policies when using their services.",
                            "Task Gullak is not responsible for the independent policies or availability of third-party services that are outside our control."
                        )
                    )
                }
            }

            // SECTION 12
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "12",
                        title = "Changes to These Terms",
                        paragraphs = listOf(
                            "We may update these Terms & Conditions when necessary to reflect changes in our services, technology, legal requirements, security practices or business operations.",
                            "Updated Terms may be published on the Task Gullak website or communicated through other reasonable methods.",
                            "If you continue to use Task Gullak after updated Terms become effective, your continued use may mean that you accept the updated Terms."
                        ),
                        infoBox = "Keep checking this page" to "We recommend reviewing these Terms from time to time so you remain aware of the current rules."
                    )
                }
            }

            // SECTION 13 + FINAL + BACK BUTTON + FOOTER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clip(RoundedCornerShape(bottomStart = 21.dp, bottomEnd = 21.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFBFE0F1).copy(alpha = 0.86f),
                            shape = RoundedCornerShape(bottomStart = 21.dp, bottomEnd = 21.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 22.dp)
                ) {
                    LegalSectionArticle(
                        number = "13",
                        title = "Contact Us",
                        paragraphs = listOf(
                            "If you have questions about these Terms, task requirements, account restrictions or other matters related to Task Gullak, you can contact our support team through the official support channel provided on the platform at nestreview88@gmail.com."
                        ),
                        contactBox = "Need help?" to "When contacting support, provide enough information for us to understand your question or issue."
                    )

                    // FINAL MESSAGE (.terms-final)
                    LegalFinalBox(
                        boldTitle = "Use Task Gullak fairly.",
                        bodyText = "Complete genuine tasks, submit accurate proof and follow the requirements shown on the platform."
                    )

                    // BACK BUTTON (.terms-actions)
                    LegalBackButton(
                        text = backButtonLabel,
                        onClick = onBack,
                        testTag = "terms_back_button"
                    )

                    // FOOTER (.terms-footer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 19.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms & Conditions",
                            color = TgLegalBlue600,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "  •  ",
                            color = TgLegalMuted,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Privacy Policy",
                            color = TgLegalMuted,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clickable { onSwitchToPrivacy() }
                                .testTag("terms_switch_to_privacy")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(34.dp))
            }
        }
    }
}

/**
 * Exact 100% UI replica of privacy.html + privacy.css
 */
@Composable
fun PrivacyScreen(
    fromSource: String = "login",
    onBack: () -> Unit,
    onSwitchToTerms: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val backButtonLabel = if (fromSource == "signup") "Back to Sign Up" else "Back to Login"

    val tocItems = listOf(
        "01. Information We Collect" to 1,
        "02. How We Use Your Information" to 2,
        "03. Task & Submission Data" to 3,
        "04. Device & Technical Information" to 4,
        "05. Cookies & Similar Technologies" to 5,
        "06. Information Sharing" to 6,
        "07. Data Security" to 7,
        "08. Data Retention" to 8,
        "09. Your Privacy Rights" to 9,
        "10. Children's Privacy" to 10,
        "11. Third-Party Services" to 11,
        "12. Changes to This Policy" to 12,
        "13. Contact Us" to 13
    )

    LegalSkyBackground(testTag = "privacy_screen") {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clip(RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFBFE0F1).copy(alpha = 0.86f),
                            shape = RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LegalHeaderBlock(
                        badgeText = "PRIVACY & DATA",
                        title = "Privacy Policy",
                        subtitle = "Learn how Task Gullak collects, uses, protects, and manages your information.",
                        metaRight = "Last updated: 2026"
                    )

                    LegalIntroBox(
                        paragraphs = listOf(
                            "Your privacy matters to us. This Privacy Policy explains what information Task Gullak may collect when you use our website, services, tasks, and related features.",
                            "By using Task Gullak, you acknowledge that your information may be collected and processed as described in this Privacy Policy."
                        )
                    )

                    LegalTocGrid(
                        heading = "Quick Overview",
                        items = tocItems,
                        onSelectSection = { sectionIndex ->
                            coroutineScope.launch {
                                listState.animateScrollToItem(sectionIndex)
                            }
                        }
                    )
                }
            }

            // SECTION 01
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "01",
                        title = "Information We Collect",
                        paragraphs = listOf(
                            "Task Gullak may collect information that you provide directly when creating an account, using our services, participating in tasks, or contacting us."
                        ),
                        bullets = listOf(
                            "Name or display name",
                            "Email address",
                            "Account and authentication information",
                            "Task participation and activity information",
                            "Information you voluntarily submit"
                        ),
                        infoBox = "Account Information" to "We collect only the information reasonably required to operate your Task Gullak account, provide services, and maintain platform security."
                    )
                }
            }

            // SECTION 02
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "02",
                        title = "How We Use Your Information",
                        paragraphs = listOf(
                            "Information collected through Task Gullak may be used to operate and improve the platform, manage accounts, process tasks, and provide relevant services."
                        ),
                        bullets = listOf(
                            "Create and manage your account",
                            "Authenticate your identity",
                            "Provide available tasks and services",
                            "Process task submissions and verification",
                            "Communicate important account information",
                            "Detect fraud, abuse, or suspicious activity",
                            "Improve platform performance and security"
                        )
                    )
                }
            }

            // SECTION 03
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "03",
                        title = "Task & Submission Data",
                        paragraphs = listOf(
                            "When you participate in a task, you may provide information required to complete and verify that task.",
                            "Depending on the task, this information may include screenshots, images, written responses, timestamps, task results, feedback, or other proof submitted through the platform."
                        ),
                        warningBox = "Important" to "Providing information for a task does not automatically guarantee payment or approval. Submitted work may need to pass the applicable verification requirements."
                    )
                }
            }

            // SECTION 04
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "04",
                        title = "Device & Technical Information",
                        paragraphs = listOf(
                            "To maintain security, reliability, and performance, Task Gullak or its service providers may process limited technical information associated with your use of the platform."
                        ),
                        bullets = listOf(
                            "Browser or application information",
                            "Device type and operating information",
                            "IP address or network information",
                            "Basic diagnostic information",
                            "Security and activity logs"
                        )
                    )
                }
            }

            // SECTION 05
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "05",
                        title = "Cookies & Similar Technologies",
                        paragraphs = listOf(
                            "Task Gullak and its service providers may use cookies or similar technologies to support essential website functions, security, preferences, and performance."
                        ),
                        bullets = listOf(
                            "Maintain essential website functionality",
                            "Remember selected preferences",
                            "Help protect accounts and services",
                            "Understand basic platform performance"
                        ),
                        infoBox = "Your Browser" to "You may be able to control cookies through your browser settings. Disabling certain technologies may affect some platform features."
                    )
                }
            }

            // SECTION 06
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "06",
                        title = "Information Sharing",
                        paragraphs = listOf(
                            "Task Gullak does not treat your personal information as public information. Information may be shared only when reasonably necessary to operate the platform, provide requested services, maintain security, or comply with applicable requirements."
                        ),
                        bullets = listOf(
                            "Service providers supporting Task Gullak",
                            "Authentication and infrastructure providers",
                            "Verification or task-processing partners where applicable",
                            "Authorities when legally required"
                        ),
                        warningBox = "Limited Access" to "Access to personal information should be limited to what is reasonably required for the relevant service, security, verification, or legal purpose."
                    )
                }
            }

            // SECTION 07
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "07",
                        title = "Data Security",
                        paragraphs = listOf(
                            "We take reasonable technical and organizational measures to help protect your information from unauthorized access, misuse, alteration, disclosure, or loss."
                        ),
                        securityItems = listOf(
                            "Secure authentication",
                            "Access controls",
                            "Data protection",
                            "Abuse monitoring",
                            "Account security"
                        ),
                        closingParagraph = "However, no internet-based service can guarantee absolute security. You should also protect your account credentials and avoid sharing passwords or verification information with others."
                    )
                }
            }

            // SECTION 08
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "08",
                        title = "Data Retention",
                        paragraphs = listOf(
                            "We may retain information for as long as reasonably necessary to provide our services, maintain account records, complete verification, resolve disputes, prevent abuse, meet legal obligations, or protect legitimate platform interests.",
                            "When information is no longer reasonably required, it may be deleted, anonymized, or otherwise handled according to applicable requirements."
                        )
                    )
                }
            }

            // SECTION 09
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "09",
                        title = "Your Privacy Rights",
                        paragraphs = listOf(
                            "Depending on applicable law, you may have certain rights regarding your personal information."
                        ),
                        bullets = listOf(
                            "Request access to certain personal information",
                            "Request correction of inaccurate information",
                            "Request deletion where legally applicable",
                            "Ask questions about how information is used",
                            "Raise privacy-related concerns"
                        ),
                        infoBox = "Requests" to "Some requests may be subject to verification, legal requirements, security considerations, or other applicable limitations."
                    )
                }
            }

            // SECTION 10
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "10",
                        title = "Children's Privacy",
                        paragraphs = listOf(
                            "Task Gullak is not intended for individuals who are not legally permitted to use the platform or participate in the applicable tasks.",
                            "We do not knowingly collect personal information from children in violation of applicable laws. If you believe that a child has provided personal information improperly, please contact us so that the matter can be reviewed."
                        ),
                        warningBox = "Age Requirement" to "Users must meet the applicable age and eligibility requirements before creating an account or participating in Task Gullak activities."
                    )
                }
            }

            // SECTION 11
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "11",
                        title = "Third-Party Services",
                        paragraphs = listOf(
                            "Task Gullak may use third-party services to support authentication, hosting, analytics, storage, communication, payments, security, or other platform functions.",
                            "These providers may process certain information according to their own privacy policies and the services they provide."
                        ),
                        infoBox = "External Services" to "When you interact with a third-party service through Task Gullak, that service may have its own terms, privacy practices, and data controls."
                    )
                }
            }

            // SECTION 12
            item {
                LegalSectionCardWrapper {
                    LegalSectionArticle(
                        number = "12",
                        title = "Changes to This Policy",
                        paragraphs = listOf(
                            "We may update this Privacy Policy from time to time to reflect changes to Task Gullak, our services, legal requirements, or our privacy practices.",
                            "When changes are made, the updated version will be published on the applicable Task Gullak page. Your continued use of the platform after an update may be subject to the revised Privacy Policy."
                        ),
                        infoBox = "Stay Informed" to "We recommend reviewing this page periodically so you remain aware of how your information is handled."
                    )
                }
            }

            // SECTION 13 + FINAL + BACK + FOOTER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clip(RoundedCornerShape(bottomStart = 21.dp, bottomEnd = 21.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFBFE0F1).copy(alpha = 0.86f),
                            shape = RoundedCornerShape(bottomStart = 21.dp, topEnd = 0.dp, bottomEnd = 21.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 22.dp)
                ) {
                    LegalSectionArticle(
                        number = "13",
                        title = "Contact Us",
                        paragraphs = listOf(
                            "If you have questions, concerns, or requests regarding this Privacy Policy or the handling of your information, you can contact Task Gullak through the official support channel provided by the platform at nestreview88@gmail.com."
                        ),
                        contactBox = "Privacy Questions" to "When contacting us, please include enough information for us to understand and properly review your request."
                    )

                    LegalFinalBox(
                        boldTitle = "Your privacy matters.",
                        bodyText = "Task Gullak aims to handle user information responsibly and use it only for legitimate platform, security, verification, and service purposes."
                    )

                    LegalBackButton(
                        text = backButtonLabel,
                        onClick = onBack,
                        testTag = "privacy_back_button"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 19.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms & Conditions",
                            color = TgLegalMuted,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clickable { onSwitchToTerms() }
                                .testTag("privacy_switch_to_terms")
                        )
                        Text(
                            text = "  •  ",
                            color = TgLegalMuted,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Privacy Policy",
                            color = TgLegalBlue600,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "  •  ",
                            color = TgLegalMuted,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "© 2026 Task Gullak",
                            color = TgLegalMuted,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 9.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(34.dp))
            }
        }
    }
}

@Composable
private fun LegalSkyBackground(
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF4FBFF),
                        Color(0xFFEDF9FF),
                        Color(0xFFF9FCFF)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(testTag)
    ) {
        CssGridOverlay(
            gridSizeDp = 34.dp,
            lineColor = Color(0x0E1597ED)
        )

        // Floating 3D Balls (.ball-one .. .ball-five)
        Floating3DOrb(
            size = 78.dp,
            colors = listOf(Color.White, Color(0xFF61C3F8), Color(0xFF1597ED), Color(0xFF0869AA)),
            offsetX = (-32).dp,
            offsetY = 55.dp,
            alignment = Alignment.TopStart,
            durationMillis = 4800
        )
        Floating3DOrb(
            size = 64.dp,
            colors = listOf(Color.White, Color(0xFF72C9FA), Color(0xFF1597ED), Color(0xFF0867A9)),
            offsetX = 24.dp,
            offsetY = (-75).dp,
            alignment = Alignment.BottomEnd,
            durationMillis = 4300,
            delayMillis = 900
        )
        Floating3DOrb(
            size = 42.dp,
            colors = listOf(Color.White, Color(0xFFFFB071), Color(0xFFFF8730), Color(0xFFD96015)),
            offsetX = (-18).dp,
            offsetY = 140.dp,
            alignment = Alignment.TopEnd,
            durationMillis = 3900,
            delayMillis = 500
        )
        Floating3DOrb(
            size = 34.dp,
            colors = listOf(Color(0xFFFFFEF0), Color(0xFFF6C63E), Color(0xFFD59C11)),
            offsetX = 14.dp,
            offsetY = 0.dp,
            alignment = Alignment.CenterStart,
            durationMillis = 4500,
            delayMillis = 1200
        )

        content()
    }
}

@Composable
private fun LegalSectionCardWrapper(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 16.dp)
    ) {
        content()
    }
}

@Composable
private fun LegalHeaderBlock(
    badgeText: String,
    title: String,
    subtitle: String,
    metaRight: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "legalBlink")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFEEF8FF))
                .border(1.dp, Color(0xFFD4ECFA), RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(TgLegalOrange)
            )
            Text(
                text = badgeText,
                color = TgLegalBlue600,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(11.dp))

        Text(
            text = title,
            style = TextStyle(
                brush = Brush.horizontalGradient(
                    colors = listOf(TgLegalBlue600, TgLegalBlue500, TgLegalOrange)
                ),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            color = TgLegalTextSoft,
            fontFamily = PoppinsFontFamily,
            fontSize = 11.5.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(11.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Task Gullak",
                color = TgLegalMuted,
                fontFamily = PoppinsFontFamily,
                fontSize = 9.5.sp
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(TgLegalOrange)
            )
            Text(
                text = metaRight,
                color = TgLegalMuted,
                fontFamily = PoppinsFontFamily,
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
private fun LegalIntroBox(paragraphs: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 17.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF1FAFF), Color.White)
                )
            )
            .border(1.dp, Color(0xFFD9EDF8), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paragraphs.forEach { p ->
            Text(
                text = p,
                color = TgLegalTextSoft,
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun LegalTocGrid(
    heading: String,
    items: List<Pair<String, Int>>,
    onSelectSection: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FCFF))
            .border(1.dp, Color(0xFFDCEEF8), RoundedCornerShape(16.dp))
            .padding(15.dp)
    ) {
        Text(
            text = heading,
            color = TgLegalText,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val pairs = items.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            pairs.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (label, idx) ->
                        Text(
                            text = label,
                            color = TgLegalBlue600,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectSection(idx) }
                                .padding(vertical = 2.dp)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegalSectionArticle(
    number: String,
    title: String,
    paragraphs: List<String>,
    paymentWarning: Pair<String, String>? = null,
    extraParagraphsAfterPaymentWarning: List<String> = emptyList(),
    bullets: List<String> = emptyList(),
    securityItems: List<String> = emptyList(),
    closingParagraph: String? = null,
    infoBox: Pair<String, String>? = null,
    warningBox: Pair<String, String>? = null,
    contactBox: Pair<String, String>? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Section Heading (.section-heading)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(bottom = 9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(TgLegalBlue100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = TgLegalBlue600,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                )
            }
            Text(
                text = title,
                color = TgLegalText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        paragraphs.forEachIndexed { idx, p ->
            Text(
                text = p,
                color = TgLegalTextSoft,
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = if (idx == 0) 0.dp else 7.dp)
            )
        }

        // Payment Warning (.payment-warning)
        if (paymentWarning != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFF9DF), Color(0xFFFFFDF1))
                        )
                    )
                    .border(1.dp, Color(0xFFF0DDA0), RoundedCornerShape(15.dp))
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(86.dp)
                        .background(TgLegalOrange)
                )
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = paymentWarning.first,
                        color = Color(0xFF856500),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = paymentWarning.second,
                        color = Color(0xFF705C17),
                        fontFamily = PoppinsFontFamily,
                        fontSize = 10.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        extraParagraphsAfterPaymentWarning.forEach { p ->
            Text(
                text = p,
                color = TgLegalTextSoft,
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 7.dp)
            )
        }

        // Bullet list (.terms-section ul / .privacy-section ul)
        if (bullets.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bullets.forEach { bullet ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TgLegalBlue400)
                        )
                        Text(
                            text = bullet,
                            color = TgLegalTextSoft,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Security Box (.security-box)
        if (securityItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TgLegalGreenSoft)
                    .border(1.dp, TgLegalGreenBorder, RoundedCornerShape(14.dp))
                    .padding(13.dp)
            ) {
                Text(
                    text = "Security Practices",
                    color = Color(0xFF18734E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    securityItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD6EEE2), RoundedCornerShape(999.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = item,
                                color = Color(0xFF31765A),
                                fontFamily = PoppinsFontFamily,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        if (closingParagraph != null) {
            Text(
                text = closingParagraph,
                color = TgLegalTextSoft,
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Blue Info Box (.info-box)
        if (infoBox != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F9FF))
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(68.dp)
                        .background(TgLegalBlue400)
                )
                Column(modifier = Modifier.padding(13.dp)) {
                    Text(
                        text = infoBox.first,
                        color = TgLegalBlue700,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Text(
                        text = infoBox.second,
                        color = TgLegalTextSoft,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 10.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Yellow Warning Box (.warning-box)
        if (warningBox != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TgLegalYellowSoft)
                    .border(1.dp, TgLegalYellowBorder, RoundedCornerShape(14.dp))
                    .padding(13.dp)
            ) {
                Text(
                    text = warningBox.first.uppercase(),
                    color = Color(0xFF977300),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Text(
                    text = warningBox.second,
                    color = Color(0xFF705C17),
                    fontFamily = PoppinsFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Orange Contact Box (.contact-box)
        if (contactBox != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TgLegalOrangeSoft)
                    .border(1.dp, TgLegalOrangeBorder, RoundedCornerShape(14.dp))
                    .padding(13.dp)
            ) {
                Text(
                    text = contactBox.first,
                    color = Color(0xFFC8611C),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Text(
                    text = contactBox.second,
                    color = TgLegalTextSoft,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 17.sp
                )
            }
        }

        HorizontalDivider(
            color = Color(0xFFE7F1F6),
            modifier = Modifier.padding(top = 18.dp)
        )
    }
}

@Composable
private fun LegalFinalBox(
    boldTitle: String,
    bodyText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF0F9FF), Color(0xFFFFFDF1))
                )
            )
            .border(1.dp, Color(0xFFDCEEF7), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TgLegalGreenSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                color = TgLegalGreen,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Column {
            Text(
                text = boldTitle,
                color = TgLegalText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = bodyText,
                color = TgLegalTextSoft,
                fontFamily = PoppinsFontFamily,
                fontSize = 10.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LegalBackButton(
    text: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(41.dp)
                .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = TgLegalBlue500)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(TgLegalBlue500, TgLegalBlue600)
                    )
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp
            )
        }
    }
}
