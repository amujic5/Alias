package hr.azzi.socialgames.alias.Service

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import hr.azzi.socialgames.alias.Models.Dictionary

class DatabaseUtil {
    companion object {
        private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

        init {
            firebaseDatabase.setPersistenceEnabled(true)
        }

        fun getDatabase() : FirebaseDatabase {
            return firebaseDatabase
        }
    }
}

class DictionaryService {

    companion object {
        val instance = DictionaryService()
        var playingDictionary: Dictionary? = null
    }

    private val childRef by lazy {
        DatabaseUtil.getDatabase().getReference("dictionaries")
    }

    var dictionaries: MutableList<Dictionary> = mutableListOf()

    init {

        childRef.child("0").child("")

        childRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                val objects = (p0.value as ArrayList<Any>)

                objects.forEach {
                    if (it is Map<*, *>) {
                        val json: Map<String, Any> = it as Map<String, Any>
                        val dic = Dictionary.dictionaryFromJSON(json)
                        val size = dic.words.size
                        dictionaries.add(dic)
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    fun fetchWords(language: String, callback: (ArrayList<String>) -> Unit) {

        DatabaseUtil.getDatabase().getReference("words").child(language).addValueEventListener(object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                val value = p0.value
                Log.w(TAG, "vvvv $value")
                val words = (p0.value as ArrayList<String>)
                callback(words)
            }

            override fun onCancelled(error: DatabaseError) {
                val emptyWords: ArrayList<String> = ArrayList()
                callback(emptyWords)
            }
        })
    }

    fun fetchDictionaries(callback: (MutableList<Dictionary>) -> Unit) {

        if (!dictionaries.isEmpty()) {
            callback(dictionaries)
            return
        } else {

            childRef.addValueEventListener(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {
                    var localDictionaries: MutableList<Dictionary> = mutableListOf()
                    val objects = (p0.value as ArrayList<Any>)

                    objects.forEach {
                        if (it is Map<*, *>) {
                            val json: Map<String, Any> = it as Map<String, Any>
                            val dic = Dictionary.dictionaryFromJSON(json)

                            Log.w(TAG, "lalala $dic.words.size")
                            localDictionaries.add(dic)
                        }
                    }


                    dictionaries = localDictionaries
                    callback(dictionaries)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(dictionaries)
                }
            })


        }
    }

}