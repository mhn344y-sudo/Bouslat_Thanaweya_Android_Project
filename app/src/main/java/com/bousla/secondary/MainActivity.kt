package com.bousla.secondary

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Locale

private val Ink = Color(0xFF173B3A)
private val Teal = Color(0xFF287C73)
private val Canvas = Color(0xFFF4F7F5)
private val Muted = Color(0xFF687A77)

data class Track(val id: String, val name: String, val description: String, val subjects: List<String>, val tint: Color)
data class Lesson(val title: String, val summary: String, val points: List<String>, val question: String, val options: List<String>, val answer: Int, val explanation: String)
data class Subject(val name: String, val lessons: List<Lesson>, val shared: Boolean = false)

private val sharedSubjects = listOf("اللغة العربية", "اللغة الأجنبية الأولى", "التاريخ المصري", "التربية الدينية")
private val tracks = listOf(
    Track("med", "الطب وعلوم الحياة", "مسار العلوم والحياة", listOf("الرياضيات", "الفيزياء", "الكيمياء", "الأحياء"), Color(0xFF3C8B78)),
    Track("eng", "الهندسة وعلوم الحاسب", "مسار التكنولوجيا والابتكار", listOf("الرياضيات", "الفيزياء", "الكيمياء", "البرمجة وعلوم الحاسب"), Color(0xFF4776A8)),
    Track("biz", "الأعمال", "مسار الاقتصاد والإدارة", listOf("الرياضيات", "الاقتصاد", "الإحصاء", "المحاسبة"), Color(0xFFB17A3D)),
    Track("arts", "الآداب والفنون", "مسار الإنسانيات والإبداع", listOf("الجغرافيا", "علم النفس", "الفلسفة", "اللغة الأجنبية الثانية"), Color(0xFF8669A8))
)
private val demoLesson = Lesson(
    "مدخل إلى المفاهيم الأساسية",
    "هذا نموذج تعليمي تجريبي يوضح طريقة عرض الدرس داخل التطبيق. يجب استبدال المحتوى ببيانات الكتاب المدرسي الرسمي قبل الاعتماد الدراسي.",
    listOf("ابدأ بتحديد الفكرة الرئيسية للموضوع.", "اربط المفهوم بمثال من الحياة اليومية.", "راجع المصطلحات ثم اختبر فهمك."),
    "ما أفضل خطوة أولى لفهم موضوع جديد؟",
    listOf("حفظ التفاصيل دون فهم", "تحديد الفكرة الرئيسية", "تجاهل المصطلحات", "الانتقال مباشرة إلى درس آخر"),
    1,
    "تحديد الفكرة الرئيسية يساعد على تنظيم التفاصيل وربطها ببعضها."
)

private fun subjectsFor(track: Track): List<Subject> =
    (track.subjects + sharedSubjects).map { name ->
        Subject(name, listOf(demoLesson.copy(title = "الدرس الأول: مدخل إلى $name"), demoLesson.copy(title = "الدرس الثاني: تطبيقات ومراجعة")), name in sharedSubjects)
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BouslaApp(this) }
    }
}

private enum class Page { HOME, SUBJECTS, TIMER, AI, PROFILE }

@Composable
private fun BouslaApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("bousla", Context.MODE_PRIVATE) }
    var trackId by remember { mutableStateOf(prefs.getString("track", null)) }
    var page by remember { mutableStateOf(Page.HOME) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var completed by remember { mutableIntStateOf(prefs.getInt("completed", 0)) }
    val track = tracks.firstOrNull { it.id == trackId }
    val colorScheme = if (dark) darkColorScheme(primary = Color(0xFF8CCFC2), background = Color(0xFF101918), surface = Color(0xFF1A2826)) else lightColorScheme(primary = Teal, background = Canvas, surface = Color.White)
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (track == null) {
                    TrackPicker { chosen ->
                        trackId = chosen.id
                        prefs.edit().putString("track", chosen.id).apply()
                    }
                } else if (selectedLesson != null) {
                    LessonScreen(selectedLesson!!, onBack = { selectedLesson = null }, onComplete = {
                        completed += 1
                        prefs.edit().putInt("completed", completed).apply()
                    })
                } else if (selectedSubject != null) {
                    SubjectScreen(selectedSubject!!, onBack = { selectedSubject = null }) { selectedLesson = it }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        when (page) {
                            Page.HOME -> HomeScreen(track, completed) { page = Page.SUBJECTS }
                            Page.SUBJECTS -> SubjectsScreen(track) { selectedSubject = it }
                            Page.TIMER -> TimerScreen(context)
                            Page.AI -> AssistantScreen()
                            Page.PROFILE -> ProfileScreen(track, dark, onDark = {
                                dark = it
                                prefs.edit().putBoolean("dark", it).apply()
                            }, onChangeTrack = {
                                trackId = null
                                prefs.edit().remove("track").apply()
                            })
                        }
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            NavItem("الرئيسية", Icons.Default.Home, page == Page.HOME) { page = Page.HOME }
                            NavItem("المواد", Icons.Default.MenuBook, page == Page.SUBJECTS) { page = Page.SUBJECTS }
                            NavItem("المؤقت", Icons.Default.Timer, page == Page.TIMER) { page = Page.TIMER }
                            NavItem("المساعد", Icons.Default.Chat, page == Page.AI) { page = Page.AI }
                            NavItem("حسابي", Icons.Default.Person, page == Page.PROFILE) { page = Page.PROFILE }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) })
}

@Composable
private fun TrackPicker(onChoose: (Track) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text("بوصلة الثانوية", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("اختر مسارك الدراسي للبدء", color = Muted, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
        tracks.forEach { track ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onChoose(track) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(track.tint, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(track.description, color = Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                }
            }
        }
        Text("يمكن تغيير المسار لاحقًا من صفحة حسابي.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DemoNotice() {
    Surface(color = Color(0xFFFFF2D9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("محتوى تجريبي يحتاج مراجعة المنهج الرسمي", color = Color(0xFF76501A), fontSize = 11.sp, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeScreen(track: Track, completed: Int, openSubjects: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item { Header("أهلًا بك في بوصلة الثانوية", "خطتك الدراسية تبدأ بخطوة صغيرة كل يوم.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("مسارك الدراسي", color = Color(0xFFB8D7D1))
                    Text(track.name, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    Text("تابع تقدمك ونظّم وقت مذاكرتك", color = Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)); DemoNotice(); Spacer(Modifier.height(14.dp)) }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("تقدمك الدراسي", fontWeight = FontWeight.Bold)
                    Text("$completed درس مكتمل", color = Muted, modifier = Modifier.padding(top = 6.dp))
                    LinearProgressIndicator(progress = { (completed / 20f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
        }
        item {
            Button(onClick = openSubjects, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("استعرض المواد الدراسية")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowBack, null)
            }
        }
        item { Text("نصيحة اليوم", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)) }
        item { Card(shape = RoundedCornerShape(18.dp)) { Text("قسّم الدرس إلى أجزاء صغيرة، واختبر فهمك بعد كل جزء بدلًا من تأجيل المراجعة.", modifier = Modifier.padding(16.dp), color = Muted) } }
    }
}

@Composable
private fun SubjectsScreen(track: Track, onSubject: (Subject) -> Unit) {
    val subjects = remember(track) { subjectsFor(track) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item { Header("المواد الدراسية", "مواد المسار والمواد المشتركة") }
        item { DemoNotice(); Spacer(Modifier.height(14.dp)); Text("مواد مسارك", fontWeight = FontWeight.Bold) }
        items(subjects.filter { !it.shared }) { subject -> SubjectCard(subject, onSubject) }
        item { Text("المواد المشتركة", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) }
        items(subjects.filter { it.shared }) { subject -> SubjectCard(subject, onSubject) }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun SubjectCard(subject: Subject, onClick: (Subject) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick(subject) }, shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, null, tint = Teal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.Bold)
                Text("${subject.lessons.size} دروس تجريبية", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun SubjectScreen(subject: Subject, onBack: () -> Unit, onLesson: (Lesson) -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        TextButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null); Text("رجوع") }
        Header(subject.name, "الوحدات والدروس")
        DemoNotice()
        Text("الوحدة 1: مدخل ومفاهيم أساسية", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 14.dp))
        subject.lessons.forEachIndexed { index, lesson ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onLesson(lesson) }, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("1.${index + 1}", color = Teal, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(lesson.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronLeft, null)
                }
            }
        }
    }
}

@Composable
private fun LessonScreen(lesson: Lesson, onBack: () -> Unit, onComplete: () -> Unit) {
    var answer by remember { mutableIntStateOf(-1) }
    var completedOnce by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { TextButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null); Text("رجوع للمادة") } }
        item { Header(lesson.title, "ملخص الدرس") }
        item { DemoNotice(); Spacer(Modifier.height(12.dp)) }
        item { Card(shape = RoundedCornerShape(18.dp)) { Text(lesson.summary, modifier = Modifier.padding(16.dp), lineHeight = 25.sp) } }
        item { Text("أهم النقاط", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) }
        items(lesson.points) { point -> Card(Modifier.padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(13.dp)) { Icon(Icons.Default.CheckCircle, null, tint = Teal); Spacer(Modifier.width(8.dp)); Text(point) } } }
        item { Text("اختبر فهمك", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) }
        item { Text(lesson.question, fontWeight = FontWeight.SemiBold) }
        items(lesson.options.size) { index ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { answer = index }, colors = CardDefaults.cardColors(containerColor = if (answer == index) Teal.copy(alpha = .12f) else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(13.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = answer == index, onClick = { answer = index })
                    Text(lesson.options[index])
                }
            }
        }
        if (answer >= 0) {
            item {
                val correct = answer == lesson.answer
                Text(if (correct) "إجابة صحيحة!" else "ليست الإجابة الصحيحة", color = if (correct) Teal else Color(0xFFB35B43), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text(lesson.explanation, color = Muted, modifier = Modifier.padding(top = 4.dp))
                if (!completedOnce) {
                    Button(onClick = { onComplete(); completedOnce = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("تسجيل إكمال الدرس") }
                } else Text("تم تسجيل إكمال الدرس", color = Teal, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun TimerScreen(context: Context) {
    var isBreak by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(25 * 60) }
    var remaining by remember { mutableIntStateOf(duration) }
    var running by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        } else if (running && remaining == 0) {
            running = false
            if (isBreak) scheduleBreakNotification(context)
        }
    }
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("مؤقت المذاكرة", "نظّم وقتك بين التركيز والراحة")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !isBreak, onClick = { isBreak = false; running = false; duration = 25 * 60; remaining = duration }, label = { Text("مذاكرة 25 دقيقة") })
            FilterChip(selected = isBreak, onClick = { isBreak = true; running = false; duration = 5 * 60; remaining = duration }, label = { Text("استراحة") })
        }
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(230.dp).background(MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { if (duration == 0) 0f else remaining.toFloat() / duration }, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
            Text(String.format(Locale.getDefault(), "%02d:%02d", remaining / 60, remaining % 60), fontSize = 38.sp, fontWeight = FontWeight.Bold)
        }
        if (isBreak) {
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { duration = 300; remaining = 300; running = false }) { Text("5 دقائق") }
                OutlinedButton(onClick = { duration = 600; remaining = 600; running = false }) { Text("10 دقائق") }
            }
        }
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { running = !running }) { Text(if (running) "إيقاف مؤقت" else "ابدأ") }
            OutlinedButton(onClick = { running = false; remaining = duration }) { Text("إعادة ضبط") }
        }
        Text("الاستراحة محددة بحد أقصى 10 دقائق.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
        if (Build.VERSION.SDK_INT >= 33 && !permissionRequested) {
            TextButton(onClick = {
                permissionRequested = true
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("تفعيل إشعارات انتهاء الاستراحة") }
        }
    }
}

private fun scheduleBreakNotification(context: Context) {
    val intent = Intent(context, BreakAlarmReceiver::class.java)
    val pending = PendingIntent.getBroadcast(context, 2001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val at = System.currentTimeMillis() + 1500
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        else alarm.set(AlarmManager.RTC_WAKEUP, at, pending)
    } catch (_: SecurityException) {}
}

@Composable
private fun AssistantScreen() {
    var question by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Header("مساعد المذاكرة", "اسأل عن فكرة أو اطلب تبسيط درس")
        DemoNotice()
        Card(Modifier.fillMaxWidth().weight(1f).padding(top = 14.dp), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("المساعد التجريبي", color = Teal, fontWeight = FontWeight.Bold)
                Text("مرحبًا! أستطيع مساعدتك في تنظيم الأفكار وشرح المفاهيم. الاتصال بنموذج ذكاء اصطناعي غير مفعّل في هذه النسخة.", modifier = Modifier.padding(top = 10.dp), lineHeight = 24.sp)
                if (sent) {
                    Text("سؤالك: $question", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.SemiBold)
                    Text("هذا رد تجريبي. اربط خدمة ذكاء اصطناعي آمنة من الخادم للحصول على إجابة مخصصة.", color = Muted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        OutlinedTextField(value = question, onValueChange = { question = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("اكتب سؤالك") }, shape = RoundedCornerShape(14.dp))
        Button(onClick = { if (question.isNotBlank()) sent = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("إرسال") }
    }
}

@Composable
private fun ProfileScreen(track: Track, dark: Boolean, onDark: (Boolean) -> Unit, onChangeTrack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Header("حسابي", "إعدادات تجربة الدراسة")
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("المسار الحالي", color = Muted, fontSize = 12.sp)
                Text(track.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
                TextButton(onClick = onChangeTrack, modifier = Modifier.align(Alignment.Start)) { Text("تغيير المسار") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الوضع الداكن", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(checked = dark, onCheckedChange = onDark)
        }
        Text("الإشعارات", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        Text("يمكن تفعيل إذن الإشعارات من شاشة المؤقت. تذكيرات الدراسة الدورية غير مفعّلة في هذه النسخة.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        DemoNotice()
        Text("الإصدار 1.0.0 · نسخة أولية قابلة للتطوير", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 18.dp))
    }
}
