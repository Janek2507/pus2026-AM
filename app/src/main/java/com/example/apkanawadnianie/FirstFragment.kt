package com.example.apkanawadnianie

import android.app.DatePickerDialog
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
import androidx.navigation.fragment.findNavController
import com.example.apkanawadnianie.databinding.FragmentFirstBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private var waterConsumed: Int = 0
    private var dailyGoal: Int = 0
    
    private lateinit var selectedCalendar: Calendar
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedCalendar = Calendar.getInstance()
        
        checkDailyReset()
        loadWaterConsumed()
        updateDailyGoal()
        updateDateDisplay()
        
        binding.add250Btn.setOnClickListener {
            addWater(250)
        }
        
        binding.add500Btn.setOnClickListener {
            addWater(500)
        }

        binding.remove250Btn.setOnClickListener {
            removeWater(250)
        }
        
        binding.resetBtn.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        binding.closeAppBtn.setOnClickListener {
            handleExit()
        }
        
        binding.languageBtn.setOnClickListener {
            showLanguageDialog()
        }
        
        binding.personBtn.setOnClickListener {
            showUserDataDialog()
        }

        binding.chartBtn.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_StatsFragment)
        }

        binding.currentDateText.setOnClickListener {
            showDatePicker()
        }
        
        updateWaterUI()
    }

    private fun updateDateDisplay() {
        val dateStr = displayDateFormatter.format(selectedCalendar.time)
        binding.currentDateText.text = "Dzisiaj jest $dateStr"
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            
            if (!isSameDay(selectedCalendar, newCalendar)) {
                checkAndAskToSave(onComplete = {
                    selectedCalendar = newCalendar
                    loadWaterConsumed()
                    updateDateDisplay()
                    updateWaterUI()
                })
            }
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isToday(calendar: Calendar): Boolean {
        return isSameDay(calendar, Calendar.getInstance())
    }

    private fun checkAndAskToSave(onComplete: () -> Unit) {
        if (!isToday(selectedCalendar) && waterConsumed > 0) {
            val dateStr = displayDateFormatter.format(selectedCalendar.time)
            AlertDialog.Builder(requireContext())
                .setTitle("Zapisywanie")
                .setMessage("Czy chcesz zapisać $waterConsumed ml do statystyk dla dnia $dateStr?")
                .setPositiveButton("TAK") { _, _ ->
                    saveToPermanentStats()
                    resetWaterCounterInternal()
                    onComplete()
                }
                .setNegativeButton("NIE") { _, _ ->
                    onComplete()
                }
                .show()
        } else {
            onComplete()
        }
    }

    private fun handleExit() {
        if (!isToday(selectedCalendar) && waterConsumed > 0) {
            val dateStr = displayDateFormatter.format(selectedCalendar.time)
            AlertDialog.Builder(requireContext())
                .setTitle("Wyjście")
                .setMessage("Czy chcesz zapisać $waterConsumed ml do statystyk dla dnia $dateStr przed wyjściem?")
                .setPositiveButton("TAK") { _, _ ->
                    saveToPermanentStats()
                    resetWaterCounterInternal()
                    requireActivity().finish()
                }
                .setNegativeButton("NIE") { _, _ ->
                    showExitConfirmationDialog()
                }
                .setNeutralButton("Anuluj", null)
                .show()
        } else {
            showExitConfirmationDialog()
        }
    }

    private fun saveToPermanentStats() {
        val statsPref = requireActivity().getSharedPreferences("WaterStats", Context.MODE_PRIVATE)
        val dateKey = dateFormatter.format(selectedCalendar.time)
        val statsJson = statsPref.getString("daily_stats", "[]") ?: "[]"
        val statsArray = JSONArray(statsJson)
        
        var found = false
        for (i in 0 until statsArray.length()) {
            val obj = statsArray.getJSONObject(i)
            if (obj.getString("date") == dateKey) {
                obj.put("amount", waterConsumed)
                found = true
                break
            }
        }

        if (!found) {
            val newStat = JSONObject()
            newStat.put("date", dateKey)
            newStat.put("amount", waterConsumed)
            statsArray.put(newStat)
        }
        
        statsPref.edit().putString("daily_stats", statsArray.toString()).apply()
    }

    private fun checkDailyReset() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val statsPref = requireActivity().getSharedPreferences("WaterStats", Context.MODE_PRIVATE)
        
        val lastDate = sharedPref.getString("last_record_date", "") ?: ""
        val todayStr = dateFormatter.format(Date())

        if (lastDate.isNotEmpty() && lastDate != todayStr) {
            val amount = sharedPref.getInt("water_consumed_$lastDate", 0)
            if (amount > 0) {
                // Save to stats if not already there or update
                val statsJson = statsPref.getString("daily_stats", "[]") ?: "[]"
                val statsArray = JSONArray(statsJson)
                
                var found = false
                for (i in 0 until statsArray.length()) {
                    val obj = statsArray.getJSONObject(i)
                    if (obj.getString("date") == lastDate) {
                        obj.put("amount", amount)
                        found = true
                        break
                    }
                }
                if (!found) {
                    val newStat = JSONObject()
                    newStat.put("date", lastDate)
                    newStat.put("amount", amount)
                    statsArray.put(newStat)
                }
                
                statsPref.edit().putString("daily_stats", statsArray.toString()).apply()
            }
            
            with(sharedPref.edit()) {
                putString("last_record_date", todayStr)
                apply()
            }
        } else if (lastDate.isEmpty()) {
            sharedPref.edit().putString("last_record_date", todayStr).apply()
        }
    }

    private fun loadWaterConsumed() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val dateKey = dateFormatter.format(selectedCalendar.time)
        waterConsumed = sharedPref.getInt("water_consumed_$dateKey", 0)
    }

    private fun saveWaterConsumed() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val dateKey = dateFormatter.format(selectedCalendar.time)
        with(sharedPref.edit()) {
            putInt("water_consumed_$dateKey", waterConsumed)
            if (isToday(selectedCalendar)) {
                putString("last_record_date", dateKey)
            }
            apply()
        }
    }

    private fun updateDailyGoal() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val age = sharedPref.getInt("user_age", -1)
        val height = sharedPref.getInt("user_height", -1)
        val weight = sharedPref.getInt("user_weight", -1)

        if (age == -1 || height == -1 || weight == -1) {
            dailyGoal = 0
            binding.dailyGoalText.text = "Proszę wprowadzić dane w prawym dolnym rogu"
        } else {
            dailyGoal = weight * 35
            binding.dailyGoalText.text = "Dzienny cel: $dailyGoal ml"
        }
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
                updateDailyGoal()
                updateWaterUI()
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
    
    private fun resetWaterCounterInternal() {
        waterConsumed = 0
        saveWaterConsumed()
        updateWaterUI()
    }

    private fun resetWaterCounter() {
        resetWaterCounterInternal()
    }
    
    private fun addWater(amount: Int) {
        waterConsumed += amount
        saveWaterConsumed()
        updateWaterUI()
    }

    private fun removeWater(amount: Int) {
        waterConsumed -= amount
        if (waterConsumed < 0) waterConsumed = 0
        saveWaterConsumed()
        updateWaterUI()
    }
    
    private fun updateWaterUI() {
        binding.waterCountText.text = "$waterConsumed ml"
        
        val colorRes = if (dailyGoal > 0 && waterConsumed >= dailyGoal) {
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
        binding.remove250Btn.backgroundTintList = colorStateList
        binding.resetBtn.imageTintList = colorStateList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}