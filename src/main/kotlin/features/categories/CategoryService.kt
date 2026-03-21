package dev.jcasas.features.categories

class CategoryService(private val repository: CategoryRepository) {

    suspend fun create(category: NewCategory): Int = repository.create(category)

    suspend fun getById(id: Int): Category? = repository.findById(id)

    suspend fun getAllActive(): List<Category> = repository.findAllActive()

    suspend fun update(id: Int, category: NewCategory) = repository.update(id, category)

    suspend fun delete(id: Int) = repository.softDelete(id)
}
