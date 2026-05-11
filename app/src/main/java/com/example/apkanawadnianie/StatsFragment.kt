package com.example.apkanawadnianie

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apkanawadnianie.databinding.FragmentStatsBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addStatBtn.setOnClickListener {
            showAddStatDialog()
        }

        setupRecyclerView()
    }

    private fun showAddStatDialog() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            showAmountDialog(dateStr)
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showAmountDialog(date: String, initialAmount: Int = 0) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Wprowadź ilość wody dla $date")

        val input = EditText(requireContext())
        input.hint = "Ilość w ml"
        if (initialAmount > 0) {
            input.setText(initialAmount.toString())
        }
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("Zapisz") { _, _ ->
            val amount = input.text.toString().toIntOrNull() ?: 0
            if (amount > 0) {
                saveOrUpdateStat(date, amount)
            }
        }
        builder.setNegativeButton("Anuluj") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun saveOrUpdateStat(date: String, amount: Int) {
        val sharedPref = requireActivity().getSharedPreferences("WaterStats", Context.MODE_PRIVATE)
        val statsJson = sharedPref.getString("daily_stats", "[]") ?: "[]"
        val statsArray = JSONArray(statsJson)
        var found = false

        for (i in 0 until statsArray.length()) {
            val obj = statsArray.getJSONObject(i)
            if (obj.getString("date") == date) {
                obj.put("amount", amount)
                found = true
                break
            }
        }

        if (!found) {
            val newStat = JSONObject()
            newStat.put("date", date)
            newStat.put("amount", amount)
            statsArray.put(newStat)
        }

        sharedPref.edit().putString("daily_stats", statsArray.toString()).apply()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val sharedPref = requireActivity().getSharedPreferences("WaterStats", Context.MODE_PRIVATE)
        val statsJson = sharedPref.getString("daily_stats", "[]") ?: "[]"
        val statsArray = JSONArray(statsJson)
        val statsList = mutableListOf<StatItem>()

        for (i in 0 until statsArray.length()) {
            val obj = statsArray.getJSONObject(i)
            statsList.add(StatItem(obj.getString("date"), obj.getInt("amount")))
        }

        // Calculate daily goal for chart coloring
        val userPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val weight = userPref.getInt("user_weight", -1)
        val dailyGoal = if (weight != -1) weight * 35 else 2000

        // Update chart
        binding.waterChart.setData(statsList.sortedBy { it.date }, dailyGoal)

        // Sort by date string descending for the list
        statsList.sortByDescending { it.date }

        if (statsList.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.statsRecyclerView.visibility = View.GONE
            binding.waterChart.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.statsRecyclerView.visibility = View.VISIBLE
            binding.waterChart.visibility = View.VISIBLE
            binding.statsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.statsRecyclerView.adapter = StatsAdapter(
                statsList,
                onDelete = { date -> deleteStat(date) },
                onEdit = { item -> showAmountDialog(item.date, item.amount) }
            )
        }
    }

    private fun deleteStat(date: String) {
        val sharedPref = requireActivity().getSharedPreferences("WaterStats", Context.MODE_PRIVATE)
        val statsJson = sharedPref.getString("daily_stats", "[]") ?: "[]"
        val statsArray = JSONArray(statsJson)
        val newArray = JSONArray()

        for (i in 0 until statsArray.length()) {
            val obj = statsArray.getJSONObject(i)
            if (obj.getString("date") != date) {
                newArray.put(obj)
            }
        }

        sharedPref.edit().putString("daily_stats", newArray.toString()).apply()
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class StatItem(val date: String, val amount: Int)

    class StatsAdapter(
        private val stats: List<StatItem>,
        private val onDelete: (String) -> Unit,
        private val onEdit: (StatItem) -> Unit
    ) : RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateText: TextView = view.findViewById(R.id.date_text)
            val amountText: TextView = view.findViewById(R.id.amount_text)
            val deleteBtn: ImageView = view.findViewById(R.id.delete_btn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stat, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = stats[position]
            holder.dateText.text = item.date
            val amountStr = "${item.amount} ml"
            holder.amountText.text = amountStr
            
            holder.itemView.setOnClickListener {
                onEdit(item)
            }

            holder.deleteBtn.setOnClickListener {
                onDelete(item.date)
            }
        }

        override fun getItemCount() = stats.size
    }
}
