package hr.azzi.socialgames.alias.Service

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import hr.azzi.socialgames.alias.Models.Dictionary

class DictionaryService {

    companion object {
        val instance = DictionaryService()
    }

    private val childRef by lazy {
        FirebaseDatabase.getInstance().getReference("dictionaries")
    }

    var dictionaries: MutableList<Dictionary> = mutableListOf()

    init {
//        childRef.keepSynced(false)
        childRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                Log.d(TAG, "vvvv is: ${p0.value}")
                val objects = (p0.value as ArrayList<Any>)

                objects.forEach {
                    if (it is Map<*, *>) {
                        val json: Map<String, Any> = it as Map<String, Any>
                        val dic = Dictionary.dictionaryFromJSON(json)
                        dictionaries.add(dic)
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

}