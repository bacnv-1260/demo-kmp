/* gkc_hash_code : 01FYBK13MRC3QWA9YSTA2809HS */
package com.demo.kmp.domain.usecase

abstract class UseCase<Type, in Params> where Type : Any? {

    protected abstract suspend fun run(params: Params): Type

    suspend fun invoke(params: Params): Type {
        return run(params)
    }

    class None
}
