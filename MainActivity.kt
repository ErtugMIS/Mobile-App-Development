package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AppRepository(applicationContext)

        setContent {

            MyApplicationTheme {

                GenericCrudApp(repository)
            }
        }
    }
}


// Screen

enum class Screen {
    LOGIN,
    REGISTER,
    LIST,
    DETAIL,
    EDIT
}

enum class FilterType {
    ALL,
    ACTIVE,
    PASSIVE
}

enum class SortType {
    NEWEST,
    OLDEST,
    TITLE
}


@Composable
fun GenericCrudApp(
    repository: AppRepository
) {

    var currentScreen by remember {

        mutableStateOf(
            if (repository.getSessionUserId() != null) {

                Screen.LIST

            } else {

                Screen.LOGIN
            }
        )
    }

    var selectedItemId by remember {
        mutableStateOf<Long?>(null)
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var loginMessage by remember {
        mutableStateOf("")
    }

    val userId = repository.getSessionUserId()


    //Back

    BackHandler {

        when (currentScreen) {

            Screen.LOGIN -> {
                // Do nothing
            }

            Screen.REGISTER -> {

                currentScreen = Screen.LOGIN
            }

            Screen.DETAIL -> {

                currentScreen = Screen.LIST
            }

            Screen.EDIT -> {

                currentScreen = Screen.LIST
            }

            Screen.LIST -> {
                // Stay
            }
        }
    }


    // Login

    if (currentScreen == Screen.LOGIN) {

        LoginScreen(
            repository = repository,
            infoMessage = loginMessage,

            onLoginSuccess = {

                loginMessage = ""
                currentScreen = Screen.LIST
            },

            onRegisterClick = {

                loginMessage = ""
                currentScreen = Screen.REGISTER
            }
        )

        return
    }


    // Register

    if (currentScreen == Screen.REGISTER) {

        RegisterScreen(
            repository = repository,

            onBack = {

                currentScreen = Screen.LOGIN
            },

            onRegistered = {

                loginMessage =
                    "Kayıt başarılı."

                currentScreen = Screen.LOGIN
            }
        )

        return
    }


    if (userId == null) {

        currentScreen = Screen.LOGIN

        return
    }
    // List


    if (currentScreen == Screen.LIST) {

        ListScreen(
            repository = repository,
            userId = userId,
            refreshKey = refreshKey,

            onAdd = {

                selectedItemId = null
                currentScreen = Screen.EDIT
            },

            onOpenDetail = { id ->

                selectedItemId = id
                currentScreen = Screen.DETAIL
            },

            onLogout = {

                repository.logout()
                currentScreen = Screen.LOGIN
            }
        )

        return
    }


    if (currentScreen == Screen.DETAIL) {

        val item = selectedItemId?.let { id ->

            repository.getItem(
                userId = userId,
                itemId = id
            )
        }

        if (item == null) {

            currentScreen = Screen.LIST

        } else {

            DetailScreen(
                item = item,

                onBack = {

                    currentScreen = Screen.LIST
                },

                onEdit = {

                    selectedItemId = item.id
                    currentScreen = Screen.EDIT
                },

                onDelete = {

                    repository.deleteItem(
                        userId = userId,
                        itemId = item.id
                    )

                    refreshKey++

                    currentScreen = Screen.LIST
                }
            )
        }

        return
    }

    // Add Delete

    if (currentScreen == Screen.EDIT) {

        val item = selectedItemId?.let { id ->

            repository.getItem(
                userId = userId,
                itemId = id
            )
        }

        EditScreen(
            item = item,

            onBack = {

                currentScreen = Screen.LIST
            },

            onSave = { title, description, status ->

                if (item == null) {

                    repository.addItem(
                        userId = userId,
                        title = title,
                        description = description,
                        status = status
                    )

                } else {

                    repository.updateItem(
                        userId = userId,
                        itemId = item.id,
                        title = title,
                        description = description,
                        status = status
                    )
                }

                refreshKey++

                currentScreen = Screen.LIST
            }
        )
    }
}


@Composable
fun SimpleTopBar(
    title: String,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        if (showBack && onBack != null) {

            TextButton(
                onClick = onBack
            ) {

                Text("←")
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )

        if (actionText != null && onAction != null) {

            TextButton(
                onClick = onAction
            ) {

                Text(actionText)
            }
        }
    }
}

// Login

@Composable
fun LoginScreen(
    repository: AppRepository,
    infoMessage: String,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var errorMessage by rememberSaveable {
        mutableStateOf("")
    }


    Scaffold {

            padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),

            verticalArrangement =
                Arrangement.Center
        ) {

            SimpleTopBar(
                title = "Giriş"
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = "Hesabınıza giriş yapın",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = email,

                onValueChange = {

                    email = it
                    errorMessage = ""
                },

                label = {
                    Text("E-posta")
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = password,

                onValueChange = {

                    password = it
                    errorMessage = ""
                },

                label = {
                    Text("Şifre")
                },

                visualTransformation =
                    PasswordVisualTransformation(),

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            if (errorMessage.isNotEmpty()) {

                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            if (infoMessage.isNotEmpty()) {

                Text(
                    text = infoMessage
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Button(
                onClick = {

                    if (email.isBlank() ||
                        password.isBlank()
                    ) {

                        errorMessage =
                            "E-posta ve şifre zorunludur."

                    } else {

                        val result =
                            repository.login(
                                email = email,
                                password = password
                            )

                        if (result != null) {

                            onLoginSuccess()

                        } else {

                            errorMessage =
                                "Girdiğiniz E-posta veya şifre hatalı."
                        }
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Giriş Yap")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TextButton(
                onClick = onRegisterClick,

                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Hesap Oluştur")
            }
        }
    }
}


//Register

@Composable
fun RegisterScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var errorMessage by rememberSaveable {
        mutableStateOf("")
    }


    Scaffold {

            padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),

            verticalArrangement =
                Arrangement.Top
        ) {

            SimpleTopBar(
                title = "Kayıt",
                showBack = true,
                onBack = onBack
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = "Yeni hesap oluştur",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = email,

                onValueChange = {

                    email = it
                    errorMessage = ""
                },

                label = {
                    Text("E-posta")
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = password,

                onValueChange = {

                    password = it
                    errorMessage = ""
                },

                label = {
                    Text("Şifre")
                },

                visualTransformation =
                    PasswordVisualTransformation(),

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = confirmPassword,

                onValueChange = {

                    confirmPassword = it
                    errorMessage = ""
                },

                label = {
                    Text("Şifreyi tekrar giriniz")
                },

                visualTransformation =
                    PasswordVisualTransformation(),

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            if (errorMessage.isNotEmpty()) {

                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Button(
                onClick = {

                    val error =
                        repository.register(
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword
                        )

                    if (error != null) {

                        errorMessage = error

                    } else {

                        onRegistered()
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Kayıt Ol")
            }
        }
    }
}


// List

@Composable
fun ListScreen(
    repository: AppRepository,
    userId: Long,
    refreshKey: Int,
    onAdd: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onLogout: () -> Unit
) {

    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    var filter by rememberSaveable {
        mutableStateOf(FilterType.ALL)
    }

    var sort by rememberSaveable {
        mutableStateOf(SortType.NEWEST)
    }

    var filterMenuOpen by remember {
        mutableStateOf(false)
    }

    var sortMenuOpen by remember {
        mutableStateOf(false)
    }


    val allItems = remember(
        userId,
        refreshKey
    ) {

        repository.getItems(userId)
    }


    val visibleItems =
        allItems
            .filter { item ->

                val searchMatches =
                    item.title.contains(
                        searchText,
                        ignoreCase = true
                    ) ||
                            item.description.contains(
                                searchText,
                                ignoreCase = true
                            )

                val filterMatches =

                    when (filter) {

                        FilterType.ALL ->
                            true

                        FilterType.ACTIVE ->
                            item.status == "Aktif"

                        FilterType.PASSIVE ->
                            item.status == "Pasif"
                    }

                searchMatches &&
                        filterMatches
            }
            .let { list ->

                when (sort) {

                    SortType.NEWEST ->
                        list.sortedByDescending {
                            it.createdAt
                        }

                    SortType.OLDEST ->
                        list.sortedBy {
                            it.createdAt
                        }

                    SortType.TITLE ->
                        list.sortedBy {
                            it.title.lowercase()
                        }
                }
            }


    Scaffold {

            padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            SimpleTopBar(
                title = "Kayıtlar",
                actionText = "Çıkış",
                onAction = onLogout
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // SEARCH
            OutlinedTextField(
                value = searchText,

                onValueChange = {
                    searchText = it
                },

                label = {
                    Text("Ara")
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // FILTER / SORT
            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {


                // FILTER
                Box(
                    modifier = Modifier.weight(1f)
                ) {

                    Button(
                        onClick = {

                            filterMenuOpen =
                                !filterMenuOpen
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            when (filter) {

                                FilterType.ALL ->
                                    "Filtre: Tümü"

                                FilterType.ACTIVE ->
                                    "Filtre: Aktif"

                                FilterType.PASSIVE ->
                                    "Filtre: Pasif"
                            }
                        )
                    }


                    DropdownMenu(
                        expanded = filterMenuOpen,

                        onDismissRequest = {

                            filterMenuOpen = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Tümü")
                            },

                            onClick = {

                                filter =
                                    FilterType.ALL

                                filterMenuOpen =
                                    false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Aktif")
                            },

                            onClick = {

                                filter =
                                    FilterType.ACTIVE

                                filterMenuOpen =
                                    false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Pasif")
                            },

                            onClick = {

                                filter =
                                    FilterType.PASSIVE

                                filterMenuOpen =
                                    false
                            }
                        )
                    }
                }


                // SORT
                Box(
                    modifier = Modifier.weight(1f)
                ) {

                    Button(
                        onClick = {

                            sortMenuOpen =
                                !sortMenuOpen
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            when (sort) {

                                SortType.NEWEST ->
                                    "Yeni"

                                SortType.OLDEST ->
                                    "Eski"

                                SortType.TITLE ->
                                    "A-Z"
                            }
                        )
                    }


                    DropdownMenu(
                        expanded = sortMenuOpen,

                        onDismissRequest = {

                            sortMenuOpen = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Yeni → Eski")
                            },

                            onClick = {

                                sort =
                                    SortType.NEWEST

                                sortMenuOpen =
                                    false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Eski → Yeni")
                            },

                            onClick = {

                                sort =
                                    SortType.OLDEST

                                sortMenuOpen =
                                    false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("A → Z")
                            },

                            onClick = {

                                sort =
                                    SortType.TITLE

                                sortMenuOpen =
                                    false
                            }
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // ADD
            Button(
                onClick = onAdd,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Yeni Kayıt")
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // LIST
            if (visibleItems.isEmpty()) {

                Column(
                    modifier =
                        Modifier.fillMaxSize(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text("Kayıt bulunamadı.")
                }

            } else {

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = visibleItems,
                        key = { item -> item.id }
                    ) { item ->

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        onOpenDetail(item.id)
                                    }
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = item.title,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        item.description,

                                    maxLines = 2
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        "Durum: ${item.status}"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        formatDate(
                                            item.createdAt
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Detail

@Composable
fun DetailScreen(
    item: Item,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Scaffold {

            padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {

            SimpleTopBar(
                title = "Detay",
                showBack = true,
                onBack = onBack
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Text(
                text = item.title,

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Açıklama",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = item.description
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text =
                    "Durum: ${item.status}"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "Oluşturulma: ${
                        formatDate(item.createdAt)
                    }"
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Button(
                onClick = onEdit,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Düzenle")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TextButton(
                onClick = onDelete,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Sil")
            }
        }
    }
}


@Composable
fun EditScreen(
    item: Item?,
    onBack: () -> Unit,
    onSave: (
        String,
        String,
        String
    ) -> Unit
) {

    var title by rememberSaveable {

        mutableStateOf(
            item?.title ?: ""
        )
    }

    var description by rememberSaveable {

        mutableStateOf(
            item?.description ?: ""
        )
    }

    var status by rememberSaveable {

        mutableStateOf(
            item?.status ?: "Aktif"
        )
    }

    var errorMessage by rememberSaveable {

        mutableStateOf("")
    }

    var statusMenuOpen by remember {

        mutableStateOf(false)
    }


    Scaffold {

            padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {

            SimpleTopBar(
                title =
                    if (item == null)
                        "Yeni Kayıt"
                    else
                        "Kayıt Düzenle",

                showBack = true,

                onBack = onBack
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )


            // TITLE
            OutlinedTextField(
                value = title,

                onValueChange = {

                    title = it
                    errorMessage = ""
                },

                label = {
                    Text("Başlık")
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // DESCRIPTION
            OutlinedTextField(
                value = description,

                onValueChange = {

                    description = it
                    errorMessage = ""
                },

                label = {
                    Text("Açıklama")
                },

                modifier =
                    Modifier.fillMaxWidth(),

                minLines = 5
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // STATUS
            Box(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = {

                        statusMenuOpen =
                            !statusMenuOpen
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Durum: $status"
                    )
                }


                DropdownMenu(
                    expanded = statusMenuOpen,

                    onDismissRequest = {

                        statusMenuOpen =
                            false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("Aktif")
                        },

                        onClick = {

                            status = "Aktif"

                            statusMenuOpen =
                                false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Pasif")
                        },

                        onClick = {

                            status = "Pasif"

                            statusMenuOpen =
                                false
                        }
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            // ERROR
            if (errorMessage.isNotEmpty()) {

                Text(
                    text = errorMessage,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }


            // SAVE
            Button(
                onClick = {

                    if (title.trim().isEmpty()) {

                        errorMessage =
                            "Başlık boş bırakılamaz."

                    } else if (
                        description.trim().isEmpty()
                    ) {

                        errorMessage =
                            "Açıklama boş bırakılamaz."

                    } else {

                        onSave(
                            title.trim(),
                            description.trim(),
                            status
                        )
                    }
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Kaydet")
            }
        }
    }
}



fun formatDate(
    time: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        )

    return formatter.format(
        Date(time)
    )
}