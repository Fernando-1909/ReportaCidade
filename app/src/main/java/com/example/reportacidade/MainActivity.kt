package com.example.reportacidade

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.reportacidade.data.model.Notification
import com.example.reportacidade.data.model.Report
import com.example.reportacidade.data.model.ReportCategory
import com.example.reportacidade.data.model.ReportStatus
import com.example.reportacidade.data.model.User
import com.example.reportacidade.data.repository.AuthRepository
import com.example.reportacidade.data.repository.MockAuthRepositoryImpl
import com.example.reportacidade.data.repository.MockNotificationRepositoryImpl
import com.example.reportacidade.data.repository.MockReportRepositoryImpl
import com.example.reportacidade.data.repository.NotificationRepository
import com.example.reportacidade.data.repository.ReportRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.AppBarLayout
import com.example.reportacidade.utils.LocationData
import com.example.reportacidade.utils.LocationUtils
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var reportRepository: ReportRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var reportAdapter: ReportAdapter
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    private var currentUser: User = User()
    private var reportsJob: Job? = null
    private var notificationsJob: Job? = null
    
    private var currentUserIdFilter: String? = null
    private var selectedCategory: ReportCategory? = null
    private var selectedStatus: ReportStatus? = null
    private var searchText: String = ""
    private var isResolvedHistoryMode: Boolean = false

    private lateinit var chipCategory: Chip
    private lateinit var chipStatus: Chip
    private lateinit var scrollViewFilters: View

    // Variáveis para gerenciar a seleção de imagem no diálogo
    private var currentDialogImageView: ImageView? = null
    private var selectedImageUri: Uri? = null
    
    private var reportLat: Double = 0.0
    private var reportLng: Double = 0.0
    private var currentAddressField: TextInputEditText? = null

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0.0) ?: 0.0
            val address = result.data?.getStringExtra(MapPickerActivity.EXTRA_ADDRESS) ?: "Localização fixada via Mapa"
            
            reportLat = lat
            reportLng = lng
            currentAddressField?.setText(address)
            Toast.makeText(this, "Local selecionado!", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(it)
            if (savedPath != null) {
                selectedImageUri = Uri.fromFile(File(savedPath))
                currentDialogImageView?.let { iv ->
                    iv.setImageURI(selectedImageUri)
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            } else {
                Toast.makeText(this, "Erro ao processar imagem", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            
            // Decodifica o bitmap com opções de amostragem para evitar OutOfMemory
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            // Primeiro passo: ler apenas as dimensões
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calcula o inSampleSize (ex: redimensiona se maior que 1080px)
            var inSampleSize = 1
            val targetWidth = 1080
            val targetHeight = 1080
            if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                    inSampleSize *= 2
                }
            }
            
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            
            // Segundo passo: ler o bitmap real com amostragem
            val finalInputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = android.graphics.BitmapFactory.decodeStream(finalInputStream, null, options)
            finalInputStream.close()

            if (bitmap == null) return null

            val fileName = "report_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                // Comprime o bitmap (JPEG com 80% de qualidade)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MapLibre.getInstance(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        authRepository = MockAuthRepositoryImpl.getInstance(this)
        val user = authRepository.getCurrentUser()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        currentUser = user

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewReports)
        val recyclerViewNotifications: RecyclerView = findViewById(R.id.recyclerViewNotifications)
        val fab: FloatingActionButton = findViewById(R.id.fabAddReport)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        val layoutProfile: View = findViewById(R.id.layoutProfile)
        val appBarLayout: AppBarLayout = findViewById(R.id.appBarLayout)
        val tvHeaderTitle: TextView = findViewById(R.id.textViewTitleHeader)
        val editSearch: EditText = findViewById(R.id.editTextSearch)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        
        chipCategory = findViewById(R.id.chipCategory)
        chipStatus = findViewById(R.id.chipStatus)
        scrollViewFilters = findViewById(R.id.scrollViewFilters)
        
        val tvProfileName: TextView = findViewById(R.id.textViewProfileName)
        val tvProfileEmail: TextView = findViewById(R.id.textViewProfileEmail)
        val tvProfileCity: TextView = findViewById(R.id.textViewProfileCity)
        val tvProfileNeighborhood: TextView = findViewById(R.id.textViewProfileNeighborhood)
        val btnEditProfile: ImageButton = findViewById(R.id.btnEditProfile)
        val btnLogout: View = findViewById(R.id.buttonLogout)
        val btnMarkAllRead: View = findViewById(R.id.btnMarkAllRead)
        val tvSort: TextView = findViewById(R.id.textViewSort)
        val btnResolvedReports: View = findViewById(R.id.btnResolvedReports)
        val btnAboutApp: View = findViewById(R.id.btnAboutApp)
        val btnBackFromHistory: ImageButton = findViewById(R.id.btnBackFromHistory)

        reportRepository = MockReportRepositoryImpl.getInstance(this)
        notificationRepository = MockNotificationRepositoryImpl.getInstance(this)

        reportAdapter = ReportAdapter(
            currentUserId = currentUser.id,
            onReportClick = { report -> showReportDetailsDialog(report) },
            onLikeClick = { report ->
                lifecycleScope.launch {
                    reportRepository.toggleLike(report.id, currentUser.id)
                }
            }
        )
        recyclerView.adapter = reportAdapter

        notificationAdapter = NotificationAdapter(
            onNotificationClick = { notification ->
                lifecycleScope.launch {
                    notificationRepository.markAsRead(notification.id)
                    val report = reportRepository.getReportById(notification.reportId)
                    if (report != null) {
                        showReportDetailsDialog(report)
                    } else {
                        Toast.makeText(this@MainActivity, "Relato não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        recyclerViewNotifications.adapter = notificationAdapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeReports(null)

        bottomNav.setOnItemSelectedListener { item ->
            isResolvedHistoryMode = false
            btnBackFromHistory.visibility = View.GONE
            swipeRefreshLayout.isEnabled = true
            when (item.itemId) {
                R.id.nav_all_reports -> {
                    appBarLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.VISIBLE
                    recyclerViewNotifications.visibility = View.GONE
                    layoutProfile.visibility = View.GONE
                    findViewById<View>(R.id.mapFragmentContainer).visibility = View.GONE
                    fab.visibility = View.VISIBLE
                    tvHeaderTitle.text = getString(R.string.title_nearby_reports)
                    btnMarkAllRead.visibility = View.GONE
                    tvSort.visibility = View.VISIBLE
                    scrollViewFilters.visibility = View.VISIBLE
                    observeReports(null)
                    true
                }
                R.id.nav_map -> {
                    appBarLayout.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    recyclerViewNotifications.visibility = View.GONE
                    layoutProfile.visibility = View.GONE
                    findViewById<View>(R.id.mapFragmentContainer).visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE
                    btnMarkAllRead.visibility = View.GONE
                    tvSort.visibility = View.GONE
                    scrollViewFilters.visibility = View.GONE
                    swipeRefreshLayout.isEnabled = false
                    true
                }
                R.id.nav_my_reports -> {
                    appBarLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.VISIBLE
                    recyclerViewNotifications.visibility = View.GONE
                    layoutProfile.visibility = View.GONE
                    findViewById<View>(R.id.mapFragmentContainer).visibility = View.GONE
                    fab.visibility = View.VISIBLE
                    tvHeaderTitle.text = getString(R.string.title_my_reports)
                    btnMarkAllRead.visibility = View.GONE
                    tvSort.visibility = View.VISIBLE
                    scrollViewFilters.visibility = View.VISIBLE
                    observeReports(currentUser.id)
                    true
                }
                R.id.nav_notifications -> {
                    appBarLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    recyclerViewNotifications.visibility = View.VISIBLE
                    layoutProfile.visibility = View.GONE
                    findViewById<View>(R.id.mapFragmentContainer).visibility = View.GONE
                    fab.visibility = View.GONE
                    tvHeaderTitle.text = getString(R.string.title_notifications)
                    btnMarkAllRead.visibility = View.VISIBLE
                    tvSort.visibility = View.GONE
                    scrollViewFilters.visibility = View.GONE
                    observeNotifications()
                    true
                }
                R.id.nav_profile -> {
                    appBarLayout.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    recyclerViewNotifications.visibility = View.GONE
                    layoutProfile.visibility = View.VISIBLE
                    findViewById<View>(R.id.mapFragmentContainer).visibility = View.GONE
                    fab.visibility = View.GONE
                    btnMarkAllRead.visibility = View.GONE
                    tvSort.visibility = if (isResolvedHistoryMode) View.VISIBLE else View.GONE
                    scrollViewFilters.visibility = View.GONE
                    swipeRefreshLayout.isEnabled = false
                    
                    updateProfileUI(tvProfileName, tvProfileEmail, tvProfileCity, tvProfileNeighborhood)
                    true
                }
                else -> false
            }
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchText = s.toString()
                if (isResolvedHistoryMode) {
                    observeResolvedReports()
                } else {
                    observeReports(currentUserIdFilter)
                }
            }
        })

        chipCategory.setOnClickListener { showCategoryFilterDialog() }
        chipStatus.setOnClickListener { showStatusFilterDialog() }

        btnEditProfile.setOnClickListener {
            showEditProfileDialog(tvProfileName, tvProfileEmail, tvProfileCity, tvProfileNeighborhood)
        }

        btnResolvedReports.setOnClickListener {
            isResolvedHistoryMode = true
            btnBackFromHistory.visibility = View.VISIBLE
            appBarLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE
            recyclerViewNotifications.visibility = View.GONE
            layoutProfile.visibility = View.GONE
            fab.visibility = View.GONE
            tvHeaderTitle.text = getString(R.string.title_resolved_reports)
            btnMarkAllRead.visibility = View.GONE
            tvSort.visibility = View.VISIBLE
            scrollViewFilters.visibility = View.GONE
            swipeRefreshLayout.isEnabled = true
            observeResolvedReports()
        }

        btnBackFromHistory.setOnClickListener {
            isResolvedHistoryMode = false
            bottomNav.selectedItemId = R.id.nav_profile
        }

        btnAboutApp.setOnClickListener {
            val themedContext = ContextThemeWrapper(this, R.style.Theme_ReportaCidade)
            MaterialAlertDialogBuilder(themedContext)
                .setTitle(R.string.about_app_title)
                .setMessage(R.string.about_app_content)
                .setPositiveButton("OK", null)
                .show()
        }

        btnMarkAllRead.setOnClickListener {
            lifecycleScope.launch {
                notificationRepository.markAllAsRead(currentUser.id)
                Toast.makeText(this@MainActivity, "Todas as notificações marcadas como lidas", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                authRepository.signOut()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }

        fab.setOnClickListener {
            showAddOrEditReportDialog()
        }

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primary_green))
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        observeUnreadNotificationsCount(bottomNav)
    }

    private fun refreshData() {
        lifecycleScope.launch {
            // Recarrega os dados dependendo do modo atual
            if (isResolvedHistoryMode) {
                observeResolvedReports()
            } else {
                when (findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId) {
                    R.id.nav_all_reports -> observeReports(null)
                    R.id.nav_my_reports -> observeReports(currentUser.id)
                    R.id.nav_notifications -> observeNotifications()
                }
            }
            // Simula um pequeno delay para feedback visual se o carregamento for instantâneo (Mock)
            kotlinx.coroutines.delay(1000)
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this@MainActivity, "Dados atualizados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeUnreadNotificationsCount(bottomNav: BottomNavigationView) {
        lifecycleScope.launch {
            notificationRepository.getNotificationsByUser(currentUser.id).collectLatest { notifications ->
                val unreadCount = notifications.count { !it.isRead }
                val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
                if (unreadCount > 0) {
                    badge.isVisible = true
                    badge.number = unreadCount
                } else {
                    badge.isVisible = false
                }
            }
        }
    }

    private fun observeNotifications() {
        notificationsJob?.cancel()
        notificationsJob = lifecycleScope.launch {
            notificationRepository.getNotificationsByUser(currentUser.id).collect { notifications ->
                notificationAdapter.submitList(notifications)
            }
        }
    }

    private fun updateProfileUI(tvName: TextView, tvEmail: TextView, tvCity: TextView, tvNeighborhood: TextView) {
        tvName.text = currentUser.name
        tvEmail.text = currentUser.email
        tvCity.text = currentUser.city.ifBlank { "Não informado" }
        tvNeighborhood.text = currentUser.neighborhood.ifBlank { "Não informado" }
    }

    private fun showEditProfileDialog(tvName: TextView, tvEmail: TextView, tvCity: TextView, tvNeighborhood: TextView) {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ReportaCidade)
        val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.dialog_edit_profile, null)
        val editName = dialogView.findViewById<TextInputEditText>(R.id.editProfileName)
        val autoCompleteCity = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.editProfileCity)
        val autoCompleteNeighborhood = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.editProfileNeighborhood)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelEditProfile)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        editName.setText(currentUser.name)
        
        // Configurar Cidades
        val cityAdapter = ArrayAdapter(themedContext, android.R.layout.simple_dropdown_item_1line, LocationUtils.rnCities)
        autoCompleteCity.setAdapter(cityAdapter)
        autoCompleteCity.setText(currentUser.city, false)

        // Configurar Bairros iniciais
        val initialNeighborhoods = LocationUtils.neighborhoodMap[currentUser.city] ?: emptyList()
        val neighborhoodAdapter = ArrayAdapter(themedContext, android.R.layout.simple_dropdown_item_1line, initialNeighborhoods)
        autoCompleteNeighborhood.setAdapter(neighborhoodAdapter)
        autoCompleteNeighborhood.setText(currentUser.neighborhood, false)

        autoCompleteCity.setOnItemClickListener { parent, _, position, _ ->
            val cityName = parent.getItemAtPosition(position) as String
            val neighborhoods = LocationUtils.neighborhoodMap[cityName] ?: emptyList()
            val newAdapter = ArrayAdapter(themedContext, android.R.layout.simple_dropdown_item_1line, neighborhoods)
            autoCompleteNeighborhood.setText("", false)
            autoCompleteNeighborhood.setAdapter(newAdapter)

            val layoutNeighborhood = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.editProfileNeighborhoodLayout)
            if (neighborhoods.isEmpty()) {
                layoutNeighborhood.hint = "Bairros em breve..."
            } else {
                layoutNeighborhood.hint = "Bairro"
            }
        }

        val dialog = MaterialAlertDialogBuilder(themedContext)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = editName.text.toString()
            val newCity = autoCompleteCity.text.toString()
            val newNeighborhood = autoCompleteNeighborhood.text.toString()

            if (newName.isBlank() || newCity.isBlank() || newNeighborhood.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedUser = currentUser.copy(
                name = newName,
                city = newCity,
                neighborhood = newNeighborhood
            )

            lifecycleScope.launch {
                val result = authRepository.updateUserProfile(updatedUser)
                if (result.isSuccess) {
                    currentUser = updatedUser
                    updateProfileUI(tvName, tvEmail, tvCity, tvNeighborhood)
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showCategoryFilterDialog() {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ReportaCidade)
        val categories = mutableListOf("Todas")
        categories.addAll(ReportCategory.entries.map { it.displayName })

        MaterialAlertDialogBuilder(themedContext)
            .setTitle("Filtrar por Categoria")
            .setItems(categories.toTypedArray()) { _, which ->
                selectedCategory = if (which == 0) null else ReportCategory.entries[which - 1]
                chipCategory.text = if (selectedCategory == null) "Categoria" else selectedCategory!!.displayName
                chipCategory.isCloseIconVisible = selectedCategory != null
                observeReports(currentUserIdFilter)
            }
            .show()
    }

    private fun showStatusFilterDialog() {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ReportaCidade)
        val statuses = mutableListOf("Todos")
        statuses.addAll(ReportStatus.entries.map { it.displayName })

        MaterialAlertDialogBuilder(themedContext)
            .setTitle("Filtrar por Status")
            .setItems(statuses.toTypedArray()) { _, which ->
                selectedStatus = if (which == 0) null else ReportStatus.entries[which - 1]
                chipStatus.text = if (selectedStatus == null) "Status" else "Status: ${selectedStatus!!.displayName}"
                chipStatus.isCloseIconVisible = selectedStatus != null
                observeReports(currentUserIdFilter)
            }
            .show()
    }

    private fun observeReports(userId: String?) {
        currentUserIdFilter = userId
        reportsJob?.cancel()
        reportsJob = lifecycleScope.launch {
            reportRepository.getAllReports().collect { allReports ->
                val isAdmin = currentUser.id == MockAuthRepositoryImpl.ADMIN_ID
                val filtered = allReports.filter { report ->
                    // Regra: Resolvidas somem da lista geral, aparecendo apenas para Admin ou Criador
                    val isResolved = report.status == ReportStatus.RESOLVIDO
                    val canSeeResolved = isAdmin || report.userId == currentUser.id
                    
                    if (isResolved && !canSeeResolved) return@filter false
                    
                    // Se estivermos na aba "Relatos Próximos" (userId == null), não mostramos resolvidas nem para o dono/admin (para limpar a lista principal)
                    // Mas se o usuário quiser ver as resolvidas, ele vai na tela de Histórico no Perfil.
                    if (userId == null && isResolved) return@filter false

                    val matchesUser = userId == null || report.userId == userId
                    val matchesCategory = selectedCategory == null || report.category == selectedCategory
                    val matchesStatus = selectedStatus == null || report.status == selectedStatus
                    val matchesSearch = searchText.isBlank() || 
                        report.description.contains(searchText, ignoreCase = true) ||
                        report.address.contains(searchText, ignoreCase = true)
                    
                    matchesUser && matchesCategory && matchesStatus && matchesSearch
                }.sortedWith(
                    compareByDescending<Report> { it.likedBy.size }
                        .thenByDescending { it.createdAt }
                )
                
                reportAdapter.submitList(filtered)
            }
        }
    }

    private fun observeResolvedReports() {
        reportsJob?.cancel()
        reportsJob = lifecycleScope.launch {
            reportRepository.getAllReports().collect { allReports ->
                val isAdmin = currentUser.id == MockAuthRepositoryImpl.ADMIN_ID
                val filtered = allReports.filter { report ->
                    val isResolved = report.status == ReportStatus.RESOLVIDO
                    val canSee = isAdmin || report.userId == currentUser.id
                    val matchesSearch = searchText.isBlank() || 
                        report.description.contains(searchText, ignoreCase = true) ||
                        report.address.contains(searchText, ignoreCase = true)
                    
                    isResolved && canSee && matchesSearch
                }.sortedByDescending { it.createdAt }
                
                reportAdapter.submitList(filtered)
            }
        }
    }

    fun showReportDetailsDialog(report: Report) {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ReportaCidade)
        val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.dialog_report_details, null)
        
        val tvCategory = dialogView.findViewById<TextView>(R.id.detailCategory)
        val tvStatusDisplay = dialogView.findViewById<TextView>(R.id.detailStatusDisplay)
        val tvDate = dialogView.findViewById<TextView>(R.id.detailDate)
        val tvUser = dialogView.findViewById<TextView>(R.id.detailUserName)
        val ivImage = dialogView.findViewById<ImageView>(R.id.detailImage)
        val tvDescription = dialogView.findViewById<TextView>(R.id.detailDescription)
        val tvAddress = dialogView.findViewById<TextView>(R.id.detailAddress)
        val labelStatus = dialogView.findViewById<TextView>(R.id.labelStatus)
        val toggleGroup = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.statusToggleGroup)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)
        val layoutUserActions = dialogView.findViewById<LinearLayout>(R.id.layoutUserActions)
        val btnEdit = dialogView.findViewById<Button>(R.id.btnEditReport)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDeleteReport)

        tvCategory.text = report.category.displayName
        
        // Exibe o Status para todos os usuários (estilo Badge)
        tvStatusDisplay.text = report.status.displayName.uppercase()
        tvStatusDisplay.backgroundTintList = ContextCompat.getColorStateList(this, report.status.colorRes)

        // Formata e exibe a data
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        tvDate.text = sdf.format(Date(report.createdAt))

        tvUser.text = "Enviado por: ${report.userName}"
        tvDescription.text = report.description
        tvAddress.text = report.address
        
        if (report.imageUrls.isNotEmpty()) {
            try {
                ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
                ivImage.setImageURI(Uri.parse(report.imageUrls[0]))
            } catch (e: Exception) {
                ivImage.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        val currentUserAuth = authRepository.getCurrentUser()
        val isAdmin = currentUserAuth?.id == MockAuthRepositoryImpl.ADMIN_ID
        
        val dialog = MaterialAlertDialogBuilder(themedContext).setView(dialogView).create()

        // Ações de Usuário (Editar/Excluir)
        if (isAdmin || currentUserAuth?.id == report.userId) {
            layoutUserActions.visibility = View.VISIBLE
            btnEdit.visibility = if (currentUserAuth?.id == report.userId) View.VISIBLE else View.GONE
            btnDelete.visibility = View.VISIBLE
        } else {
            layoutUserActions.visibility = View.GONE
        }

        // Gerenciamento de Status (Apenas Admin)
        if (isAdmin) {
            labelStatus.visibility = View.VISIBLE
            toggleGroup.visibility = View.VISIBLE
            
            when (report.status) {
                ReportStatus.PENDENTE -> toggleGroup.check(R.id.btnPendente)
                ReportStatus.EM_ANALISE -> toggleGroup.check(R.id.btnAnalise)
                ReportStatus.RESOLVIDO -> toggleGroup.check(R.id.btnResolvido)
            }

            toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val newStatus = when (checkedId) {
                        R.id.btnPendente -> ReportStatus.PENDENTE
                        R.id.btnAnalise -> ReportStatus.EM_ANALISE
                        R.id.btnResolvido -> ReportStatus.RESOLVIDO
                        else -> report.status
                    }
                    
                    if (newStatus != report.status) {
                        lifecycleScope.launch {
                            reportRepository.updateReport(report.copy(status = newStatus))
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity, "Status atualizado!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            labelStatus.visibility = View.GONE
            toggleGroup.visibility = View.GONE
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showAddOrEditReportDialog(report)
        }

        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(themedContext)
                .setTitle(R.string.delete_report_confirm_title)
                .setMessage(R.string.delete_report_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        reportRepository.deleteReport(report.id)
                        dialog.dismiss()
                        Toast.makeText(this@MainActivity, R.string.report_deleted_success, Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    fun showAddOrEditReportDialog(existingReport: Report? = null) {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ReportaCidade)
        val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.dialog_add_report, null)
        val textViewTitle = dialogView.findViewById<TextView>(R.id.textViewTitle)
        val editDescription = dialogView.findViewById<TextInputEditText>(R.id.editTextDescription)
        val editAddress = dialogView.findViewById<TextInputEditText>(R.id.editTextAddress)
        val autoCompleteCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.imageViewSelected)
        val btnCamera = dialogView.findViewById<Button>(R.id.buttonCamera)
        val btnGallery = dialogView.findViewById<Button>(R.id.buttonGallery)
        val btnSave = dialogView.findViewById<Button>(R.id.buttonSave)
        val btnGetLocation = dialogView.findViewById<Button>(R.id.buttonGetLocation)

        currentAddressField = editAddress
        reportLat = existingReport?.latitude ?: 0.0
        reportLng = existingReport?.longitude ?: 0.0

        btnGetLocation.setOnClickListener {
            mapPickerLauncher.launch(Intent(this, MapPickerActivity::class.java))
        }

        var selectedCat: ReportCategory = existingReport?.category ?: ReportCategory.BURACO
        
        // Inicializa as referências para a seleção de imagem
        currentDialogImageView = ivPreview
        selectedImageUri = if (existingReport?.imageUrls?.isNotEmpty() == true) Uri.parse(existingReport.imageUrls[0]) else null

        textViewTitle.text = if (existingReport == null) "Nova Denúncia" else "Editar Relato"
        editDescription.setText(existingReport?.description)
        editAddress.setText(existingReport?.address)
        
        // Setup Category Dropdown
        val categories = ReportCategory.entries.map { it.displayName }
        val adapter = ArrayAdapter(themedContext, android.R.layout.simple_dropdown_item_1line, categories)
        autoCompleteCategory.setAdapter(adapter)
        autoCompleteCategory.setText(selectedCat.displayName, false)
        
        autoCompleteCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCat = ReportCategory.entries[position]
        }
        
        if (selectedImageUri != null) {
            try {
                ivPreview.setImageURI(selectedImageUri)
                ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                ivPreview.setImageResource(android.R.drawable.ic_menu_camera)
            }
        }

        btnCamera.setOnClickListener {
            // Simulação de câmera
            selectedImageUri = Uri.parse("android.resource://$packageName/${R.drawable.ic_app_logo}")
            ivPreview.setImageURI(selectedImageUri)
            ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val dialog = MaterialAlertDialogBuilder(themedContext)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            val description = editDescription.text.toString()
            val address = editAddress.text.toString()

            if (description.isBlank() || address.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val report = existingReport?.copy(
                    description = description,
                    address = address,
                    category = selectedCat,
                    latitude = reportLat,
                    longitude = reportLng,
                    imageUrls = if (selectedImageUri != null) listOf(selectedImageUri.toString()) else emptyList()
                ) ?: Report(
                    userId = currentUser.id,
                    userName = currentUser.name,
                    description = description,
                    address = address,
                    category = selectedCat,
                    latitude = reportLat,
                    longitude = reportLng,
                    imageUrls = if (selectedImageUri != null) listOf(selectedImageUri.toString()) else emptyList(),
                    createdAt = System.currentTimeMillis()
                )

                if (existingReport == null) {
                    reportRepository.createReport(report)
                    Toast.makeText(this@MainActivity, "Relato enviado!", Toast.LENGTH_SHORT).show()
                } else {
                    reportRepository.updateReport(report)
                    Toast.makeText(this@MainActivity, "Relato atualizado!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
