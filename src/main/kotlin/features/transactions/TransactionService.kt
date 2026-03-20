package dev.jcasas.features.transactions

class TransactionService(private val repository: TransactionRepository) {

    suspend fun create(transaction: NewTransaction): Int = repository.create(transaction)

    suspend fun getById(id: Int): Transaction? = repository.findById(id)

    suspend fun getAll(): List<Transaction> = repository.findAll()

    suspend fun update(id: Int, transaction: NewTransaction) = repository.update(id, transaction)

    suspend fun delete(id: Int) = repository.delete(id)
}