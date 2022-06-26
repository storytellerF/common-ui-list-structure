/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.storyteller_f.ui_list.data

import com.google.gson.annotations.SerializedName
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.database.RemoteKey

data class CommonResponse<D : Datum<RK>, RK : RemoteKey>(
    @SerializedName("total_count") val total: Int = 0,
    @SerializedName("items") val items: List<D> = emptyList(),
    val nextPage: Int? = null
)

data class SimpleResponse<D : Model>(
    @SerializedName("total_count") val total: Int = 0,
    @SerializedName("items") val items: List<D> = emptyList(),
    val nextPage: Int? = null
)
