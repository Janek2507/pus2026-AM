package com.example.apkanawadnianie

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.apkanawadnianie.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private var waterConsumed: Int = 0
    private val dailyGoal: Int = 2000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.add250Btn.setOnClickListener {
            addWater(250)
        }
        
        binding.add500Btn.setOnClickListener {
            addWater(500)
        }
        
        binding.resetBtn.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        binding.closeAppBtn.setOnClickListener {
            showExitConfirmationDialog()
        }
        
        binding.languageBtn.setOnClickListener {
            showLanguageDialog()
        }
        
        binding.personBtn.setOnClickListener {
            showUserDataDialog()
        }
        
        updateWaterUI()
    }
    
    private fun showUserDataDialog() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        var age = sharedPref.getInt("user_age", -1)
        var height = sharedPref.getInt("user_height", -1)
        var weight = sharedPref.getInt("user_weight", -1)

        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_user_data, null)
        
        val editAge = view.findViewById<EditText>(R.id.edit_age)
        val editHeight = view.findViewById<EditText>(R.id.edit_height)
        val editWeight = view.findViewById<EditText>(R.id.edit_weight)
        val btnReset = view.findViewById<TextView>(R.id.btn_reset_data)

        // Set existing values if they exist and disable editing
        if (age != -1) {
            editAge.setText(age.toString())
            editAge.isEnabled = false
        }
        if (height != -1) {
            editHeight.setText(height.toString())
            editHeight.isEnabled = false
        }
        if (weight != -1) {
            editWeight.setText(weight.toString())
            editWeight.isEnabled = false
        }

        builder.setView(view)
        
        val dialog = builder.create()

        btnReset.setOnClickListener {
            with(sharedPref.edit()) {
                remove("user_age")
                remove("user_height")
                remove("user_weight")
                apply()
            }
            editAge.text.clear()
            editHeight.text.clear()
            editWeight.text.clear()
            editAge.isEnabled = true
            editHeight.isEnabled = true
            editWeight.isEnabled = true
        }

        builder.setPositiveButton("Zapisz") { d, _ ->
            if (editAge.isEnabled) {
                val newAge = editAge.text.toString().toIntOrNull() ?: -1
                val newHeight = editHeight.text.toString().toIntOrNull() ?: -1
                val newWeight = editWeight.text.toString().toIntOrNull() ?: -1
                
                with(sharedPref.edit()) {
                    putInt("user_age", newAge)
                    putInt("user_height", newHeight)
                    putInt("user_weight", newWeight)
                    apply()
                }
            }
            d.dismiss()
        }
        
        builder.setNegativeButton("Anuluj") { d, _ ->
            d.dismiss()
        }
        
        builder.show()
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf("Polski", "Coming soon...")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Wybierz język")
        builder.setItems(languages) { dialog, which ->
            if (which == 1) { // "Coming soon..." clicked
                showEnglishComingSoonDialog()
            }
            dialog.dismiss()
        }
        builder.show()
    }
    
    private fun showEnglishComingSoonDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_coming_soon, null)
        
        builder.setView(view)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }
    
    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Wyjście")
        builder.setMessage("Czy napewno chcesz wyjść?")
        
        builder.setPositiveButton("TAK") { _, _ ->
            requireActivity().finish()
        }
        
        builder.setNegativeButton("NIE") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun showResetConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reset")
        builder.setMessage("Czy napewno chcesz zresetować swój dzienny wynik?")
        
        builder.setPositiveButton("TAK") { dialog, _ ->
            resetWaterCounter()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("NIE") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun resetWaterCounter() {
        waterConsumed = 0
        updateWaterUI()
    }
    
    private fun addWater(amount: Int) {
        waterConsumed += amount
        updateWaterUI()
    }
    
    private fun updateWaterUI() {
        binding.waterCountText.text = "$waterConsumed ml"
        
        val colorRes = if (waterConsumed >= dailyGoal) {
            R.color.dark_blue
        } else {
            R.color.water_blue
        }
        
        val color = ContextCompat.getColor(requireContext(), colorRes)
        val colorStateList = ColorStateList.valueOf(color)
        
        // Update colors of blue elements
        binding.waterDrop.imageTintList = colorStateList
        binding.dailyGoalText.setTextColor(color)
        binding.add250Btn.backgroundTintList = colorStateList
        binding.add500Btn.backgroundTintList = colorStateList
        binding.resetBtn.imageTintList = colorStateList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}