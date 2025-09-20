package com.example.ecolim.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.ecolim.EcolimApplication
import com.example.ecolim.databinding.FragmentGalleryBinding
import com.example.ecolim.ui.ViewModelFactory
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class GalleryFragment : Fragment(), DatePickerDialog.OnDateSetListener {

    private var _binding: FragmentGalleryBinding? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var isStartDatePicker = true
    private lateinit var galleryViewModel: GalleryViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().application as EcolimApplication
        val factory = ViewModelFactory(application.serverConfigManager)
        galleryViewModel = ViewModelProvider(this, factory)[GalleryViewModel::class.java]
        
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupDatePickers()

        return root
    }

    private fun setupDatePickers() {
        // Configurar click listeners para los campos de fecha
        binding.editStartDate.setOnClickListener {
            isStartDatePicker = true
            showDatePicker()
        }

        binding.editEndDate.setOnClickListener {
            isStartDatePicker = false
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog.newInstance(
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Configurar tema
        datePickerDialog.accentColor = resources.getColor(android.R.color.holo_blue_dark, null)
        datePickerDialog.show(parentFragmentManager, "DatePickerDialog")
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, monthOfYear, dayOfMonth)
        val selectedDate = dateFormat.format(calendar.time)

        if (isStartDatePicker) {
            binding.editStartDate.setText(selectedDate)
        } else {
            binding.editEndDate.setText(selectedDate)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}