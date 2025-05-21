package space.resolvingcode.eStatMCP

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlChildrenName

val ESTAT_API_KEY: String = System.getenv("ESTAT_API_KEY") ?: throw IllegalStateException("ESTAT_API_KEY environment variable is not found.")

data class eStatResponse(
	val isError: Boolean,
	val textContents: List<String>
)

// Extension function to fetch a list of table
suspend fun HttpClient.getTables(params: Map<String, String>): eStatResponse {
	// URI for tables
	val uri = "/rest/3.0/app/getStatsList"
	// Request the list of table from the API with the parameters
	val tables = this.get(uri) {
		parameter("appId", ESTAT_API_KEY)
		params.forEach { param ->
			parameter(param.key, param.value)
		}
		parameter("explanationGetFlg", "N")
		if (!params.containsKey("limit") && responseSize != null) parameter("limit", responseSize)
	}.body<StatsList>()

	if (tables.result.STATUS != 0 || tables.dataListInf == null) {
		// Return the error message if an error occurred
		return eStatResponse(true, listOf(tables.result.ERROR_MSG))
	}
	else {
		val text = tables.dataListInf.resultInf.NEXT_KEY?.let {
			"This is a part of results. The request with 'startPosition: ${tables.dataListInf.resultInf.NEXT_KEY}' will receive the rest."
		}

		val tableList = tables.dataListInf.Tables.distinctBy{it.toString()}.map {it.toList()}
		val tableGrouped = tableList.groupBy{it.last()}.mapValues{it.value.groupBy{tbl -> tbl[tbl.lastIndex - 1]}.mapValues{mEnt -> mEnt.value.map{tbl -> tbl.dropLast(2)}}}
		val jsonList = tableGrouped.mapValues{m -> m.value.mapValues{s -> s.value.map{tbl -> "{\"statsDataId\": ${tbl[0]}, \"title\": ${tbl[1]}}"}}}

		return eStatResponse(false, listOfNotNull(jsonList.toString().replace("=", ":"), text))
	}
}

// Data class representing the tables response from the API
@Serializable
@SerialName("GET_STATS_LIST")
data class StatsList(
	val result: RESULT, val parameter: PARAMETER, val dataListInf: DATALIST_INF? = null
) {
	@Serializable
	data class PARAMETER(
		@XmlElement val LANG: String, @XmlElement val DATA_FORMAT: String? = null, @XmlElement val SURVEY_YEARS: String? = null,
		@XmlElement val OPEN_YEARS: String? = null, @XmlElement val STATS_FIELD: String? = null, @XmlElement val STATS_CODE: String? = null,
		@XmlElement val SMALL_AREA: Int? = null, @XmlElement val SEARCH_WORD: String? = null, @XmlElement val SEARCH_KIND: String? = null,
		@XmlElement val COLLECT_AREA: String? = null, @XmlElement val EXPLANATION_GET_FLG: String? = null,
		@XmlElement val STATS_NAME_LIST: String? = null, @XmlElement val START_POSITION: String? = null, @XmlElement val LIMIT: Int? = null,
		@XmlElement val UPDATED_DATE: String? = null
	)

	@Serializable
	data class DATALIST_INF(
		val number: NUMBER, val resultInf: RESULT_INF, @SerialName("TABLE_INF") val Tables: List<TableInf>, val listInf: List<LIST_INF>? = null
	){
		@Serializable
		data class NUMBER(@XmlValue val value: Int)
		@Serializable
		data class RESULT_INF(
			@XmlElement val FROM_NUMBER: Int = 0, @XmlElement val TO_NUMBER: Int = 0, @XmlElement val NEXT_KEY: Int? = null
		)
		@Serializable
		data class LIST_INF(
			val id: String, @XmlElement val STAT_NAME: String, @XmlElement val GOV_ORG: String
		){
			fun toList(): List<String> {
				return listOf("\"$id\"", "\"$STAT_NAME\"", "\"$GOV_ORG\"")
			}
		}
	}
}

@Serializable
data class RESULT(
	@XmlElement val STATUS: Int, @XmlElement val ERROR_MSG: String, @XmlElement val DATE: String
)

@Serializable
@SerialName("TABLE_INF")
data class TableInf(
	val id: String, val statName: STAT_NAME, val govOrg: GOV_ORG, @XmlElement val STATISTICS_NAME: String, val title: TITLE,
	@XmlElement val CYCLE: String, @XmlElement val SURVEY_DATE: String, @XmlElement val OPEN_DATE: String, @XmlElement val SMALL_AREA: Int, @XmlElement val COLLECT_AREA: String,
	val mainCategory: MAIN_CATEGORY, val subCategory: SUB_CATEGORY, @XmlElement val OVERALL_TOTAL_NUMBER: Int,
	@XmlElement val UPDATED_DATE: String, val statisticsNameSpec: STATISTICS_NAME_SPEC, val description: DESCRIPTION? = null, val titleSpec: TITLE_SPEC,
	@XmlElement val releaseCount: Int? = null
) {
	@Serializable
	data class STAT_NAME(
		val code: String, @XmlValue val value: String
	)
	@Serializable
	data class GOV_ORG(
		val code: String, @XmlValue val value: String
	)
	@Serializable
	data class TITLE(
		val no: String? = null, @XmlValue val value: String
	)
	@Serializable
	data class MAIN_CATEGORY(
		val code: String, @XmlValue val value: String
	)
	@Serializable
	data class SUB_CATEGORY(
		val code: String, @XmlValue val value: String
	)
	@Serializable
	data class STATISTICS_NAME_SPEC(
		@XmlElement val TABULATION_CATEGORY: String, @XmlElement val TABULATION_SUB_CATEGORY1: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY2: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY3: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY4: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY5: String? = null
	)
	@Serializable
	data class DESCRIPTION(
		@XmlElement val TABULATION_CATEGORY_EXPLANATION: String, @XmlElement val TABULATION_SUB_CATEGORY_EXPLANATION1: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY_EXPLANATION2: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY_EXPLANATION3: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY_EXPLANATION4: String? = null,
		@XmlElement val TABULATION_SUB_CATEGORY_EXPLANATION5: String? = null
	)
	@Serializable
	data class TITLE_SPEC(
		@XmlElement val TABLE_CATEGORY: String? = null, @XmlElement val TABLE_NAME: String,
		@XmlElement val TABLE_EXPLANATION: String? = null,
		@XmlElement val TABLE_SUB_CATEGORY1: String? = null,
		@XmlElement val TABLE_SUB_CATEGORY2: String? = null,
		@XmlElement val TABLE_SUB_CATEGORY3: String? = null
	)

	override fun toString(): String {
		return "$id,${title.no},\"${title.value}\",\"$STATISTICS_NAME\",\"${govOrg.value}\""
	}
	fun toList(): List<String> {
		return listOf("\"$id\"", "\"${title.value}\"", "\"${STATISTICS_NAME}\"", "\"${govOrg.value}\"")
	}
}

// Extension function to fetch a list of survey
suspend fun HttpClient.getSurveys(params: Map<String, String>): eStatResponse {
	// URI for tables
	val uri = "/rest/3.0/app/getStatsList"
	// Request the list of table from the API with the parameters
	val surveys = this.get(uri) {
		parameter("appId", ESTAT_API_KEY)
		params.forEach { param ->
			parameter(param.key, param.value)
		}
		parameter("statsNameList", "Y")
		if (!params.containsKey("limit") && responseSize != null) parameter("limit", responseSize)
	}.body<StatsList>()

	if (surveys.result.STATUS != 0 || surveys.dataListInf == null) {
		// Return the error message if an error occurred
		return eStatResponse(true, listOf(surveys.result.ERROR_MSG))
	}
	else {
		val text = surveys.dataListInf.resultInf.NEXT_KEY?.let {
			"This is a part of results. The request with 'startPosition: ${surveys.dataListInf.resultInf.NEXT_KEY}' will receive the rest."
		}

		val surveyList = surveys.dataListInf.listInf!!.map {it.toList()}
		val surveyGrouped = surveyList.groupBy{it.last()}.mapValues{it.value.map{svy -> "{\"statsCode\": ${svy[0]}, \"name\": ${svy[1]}}"}}

		return eStatResponse(false, listOfNotNull(surveyGrouped.toString().replace("=", ":"), text))
	}
}

// Extension function to fetch metadata of specified table
suspend fun HttpClient.getMetadata(params: Map<String, String>): eStatResponse {
	// URI for metadata
	val uri = "/rest/3.0/app/getMetaInfo"
	// Request the metadata from the API with the parameters
	val metadata = this.get(uri) {
		parameter("appId", ESTAT_API_KEY)
		params.forEach { param ->
			parameter(param.key, param.value)
		}
		parameter("explanationGetFlg", "N")
	}.body<MetaInfo>()

	if (metadata.result.STATUS != 0) {
		// Return the error message if an error occurred
		return(eStatResponse(true, listOf(metadata.result.ERROR_MSG)))
	}
	else {
		val text = metadata.metaDataInf!!.TABLE_INF.title.value

		val metaRes = metadata.metaDataInf.CLASS_INF.joinToString(",") {
			"\"${it.id}\": {\"name\": \"${it.name}\", \"code\": {" +
					it.classes.joinToString(","){cls ->
						"\"${cls.code}\": \"${cls.name}\""
					} + "}}"
			// Limit class size to save context length
//			"\"${it.id}\": {\"name\": \"${it.name}\", \"code\": {" +
//					if (it.classes.size <= 10) {
//						it.classes.joinToString(","){cls ->
//							"\"${cls.code}\": \"${cls.name}\""
//						}
//					} else {
//						it.classes.take(5).joinToString(","){cls ->
//							"\"${cls.code}\": \"${cls.name}\""
//						} + "," + it.classes.takeLast(5).joinToString(","){cls ->
//									"\"${cls.code}\": \"${cls.name}\""
//								}
//					} + "}}"
		}

		return(eStatResponse(false, listOf(text, "{$metaRes}")))
	}
}

// Data class representing the Metadata response from the API
@Serializable
@SerialName("GET_META_INFO")
data class MetaInfo(
	val result: RESULT, val parameter: PARAMETER, val metaDataInf: METADATA_INF? = null
) {
	@Serializable
	data class PARAMETER(
		@XmlElement val LANG: String, @XmlElement val STATS_DATA_ID: String, @XmlElement val DATA_FORMAT: String? = null,
		@XmlElement val EXPLANATION_GET_FLG: String? = null
	)

	@Serializable
	data class METADATA_INF(
		@XmlElement val TABLE_INF: TableInf, @XmlChildrenName("CLASS_OBJ") val CLASS_INF: List<CLASS_OBJ>
	)
}

@Serializable
data class CLASS_OBJ(
	val id: String, val name: String, val description: String? = null, @XmlElement val EXPLANATION: String? = null,
	@SerialName("CLASS") val classes: List<CLASS>
) {
	@Serializable
	data class CLASS(
		val code: String, val name: String, val level: String, val unit: String? = null,
		val parentCode: String? = null, val addInf: String? = null
	)

	val codeNameMap = buildMap {
		classes.forEach {
			put(it.code, "\"${it.name}${it.unit?.let {u -> "($u)"} ?: ""}\"")
		}
	}
}

// Extension function to fetch data of statistical table
suspend fun HttpClient.getData(params: Map<String, String>): eStatResponse {
	// URI for Data
	val uri = "/rest/3.0/app/getStatsData"
	// Request the statistical data from the API with the parameters
	val statsData = this.get(uri) {
		parameter("appId", ESTAT_API_KEY)
		parameter("explanationGetFlg", "N")
		params.forEach { param ->
			parameter(param.key, param.value)
		}
		if (!params.containsKey("limit") && responseSize != null) parameter("limit", responseSize)
	}.body<StatsDataInfo>()

	if (statsData.result.STATUS != 0 || statsData.statisticalData?.dataInf == null) {
		// Return the error message if an error occurred
		return eStatResponse(true, listOf(statsData.result.ERROR_MSG))
	}
	else {
		val text = "${statsData.statisticalData.TABLE_INF.STATISTICS_NAME} ${statsData.statisticalData.TABLE_INF.title.value}\n" +
				if (statsData.statisticalData.resultInf.NEXT_KEY != null) {
					"This is a part of results. The request with 'startPosition: ${statsData.statisticalData.resultInf.NEXT_KEY}' will receive the rest."
				} else ""

		val valueList = statsData.statisticalData.dataInf.Values.map {it.toMap()}
		val namedValueList = valueList.map { v ->
			v.map {
				if(it.key == "value") {
					it.value
				} else {
					statsData.statisticalData.idClassObjMap[it.key]?.get(it.value) ?: it.value
				}
			}
		}

		// Recursively grouping attributes from head
		fun groupByFirst(data: List<List<String>>): Any {
			if (data.first().size == 2) {
				return buildMap {
					data.forEach {put(it[0], it[1])}
				}
			}
			else {
				val grouped = data.groupBy {it.first()}
				return grouped.mapValues {groupByFirst(it.value.map{v -> v.drop(1)})}
			}
		}

		val valueListGrouped = groupByFirst(namedValueList)

		// Not using NOTES because they are uncleaned
		//val footnotes = statsData.statisticalData.dataInf.Notes.map{it.toString()} + statsData.statisticalData.dataInf.Annotations.map{it.toString()}

		val footnotes = if(statsData.statisticalData.dataInf.Annotations.isNotEmpty()) {
			"{\"footnotes\": [${statsData.statisticalData.dataInf.Annotations.joinToString(",")}]}"
		} else null

		return eStatResponse(false, listOfNotNull(text, valueListGrouped.toString().replace("=", ":"), footnotes))
	}
}

// Data class representing the statistics data response from the API
@Serializable
@SerialName("GET_STATS_DATA")
data class StatsDataInfo(
	val result: RESULT, val parameter: PARAMETER, val statisticalData: STATISTICAL_DATA? = null
) {
	@Serializable
	data class PARAMETER(
		@XmlElement val LANG: String, @XmlElement val DATASET_ID: String? = null, @XmlElement val STATS_DATA_ID: String,
		val narrowingCond: NARROWING_COND? = null, @XmlElement val DATA_FORMAT: String? = null, @XmlElement val START_POSITION: String? = null,
		@XmlElement val LIMIT: Int? = null, @XmlElement val METAGET_FLG: String? = null, @XmlElement val CNT_GET_FLG: String? = null,
		@XmlElement val EXPLANATION_GET_FLG: String? = null,  @XmlElement val ANNOTATION_GET_FLG: String? = null
	){
		@Serializable
		data class NARROWING_COND(
			@XmlElement val LEVEL_TAB_COND: String? = null, @XmlElement val CODE_TAB_SELECT: String? = null,
			@XmlElement val CODE_TAB_FROM: String? = null, @XmlElement val CODE_TAB_TO: String? = null,
			@XmlElement val LEVEL_TIME_COND: String? = null, @XmlElement val CODE_TIME_SELECT: String? = null,
			@XmlElement val CODE_TIME_FROM: String? = null, @XmlElement val CODE_TIME_TO: String? = null,
			@XmlElement val LEVEL_AREA_COND: String? = null, @XmlElement val CODE_AREA_SELECT: String? = null,
			@XmlElement val CODE_AREA_FROM: String? = null, @XmlElement val CODE_AREA_TO: String? = null,
			@XmlElement val LEVEL_CAT01_COND: String? = null, @XmlElement val CODE_CAT01_SELECT: String? = null,
			@XmlElement val CODE_CAT01_FROM: String? = null, @XmlElement val CODE_CAT01_TO: String? = null,
			@XmlElement val LEVEL_CAT02_COND: String? = null, @XmlElement val CODE_CAT02_SELECT: String? = null,
			@XmlElement val CODE_CAT02_FROM: String? = null, @XmlElement val CODE_CAT02_TO: String? = null,
			@XmlElement val LEVEL_CAT03_COND: String? = null, @XmlElement val CODE_CAT03_SELECT: String? = null,
			@XmlElement val CODE_CAT03_FROM: String? = null, @XmlElement val CODE_CAT03_TO: String? = null,
			@XmlElement val LEVEL_CAT04_COND: String? = null, @XmlElement val CODE_CAT04_SELECT: String? = null,
			@XmlElement val CODE_CAT04_FROM: String? = null, @XmlElement val CODE_CAT04_TO: String? = null,
			@XmlElement val LEVEL_CAT05_COND: String? = null, @XmlElement val CODE_CAT05_SELECT: String? = null,
			@XmlElement val CODE_CAT05_FROM: String? = null, @XmlElement val CODE_CAT05_TO: String? = null,
			@XmlElement val LEVEL_CAT06_COND: String? = null, @XmlElement val CODE_CAT06_SELECT: String? = null,
			@XmlElement val CODE_CAT06_FROM: String? = null, @XmlElement val CODE_CAT06_TO: String? = null,
			@XmlElement val LEVEL_CAT07_COND: String? = null, @XmlElement val CODE_CAT07_SELECT: String? = null,
			@XmlElement val CODE_CAT07_FROM: String? = null, @XmlElement val CODE_CAT07_TO: String? = null,
			@XmlElement val LEVEL_CAT08_COND: String? = null, @XmlElement val CODE_CAT08_SELECT: String? = null,
			@XmlElement val CODE_CAT08_FROM: String? = null, @XmlElement val CODE_CAT08_TO: String? = null,
			@XmlElement val LEVEL_CAT09_COND: String? = null, @XmlElement val CODE_CAT09_SELECT: String? = null,
			@XmlElement val CODE_CAT09_FROM: String? = null, @XmlElement val CODE_CAT09_TO: String? = null,
			@XmlElement val LEVEL_CAT10_COND: String? = null, @XmlElement val CODE_CAT10_SELECT: String? = null,
			@XmlElement val CODE_CAT10_FROM: String? = null, @XmlElement val CODE_CAT10_TO: String? = null,
			@XmlElement val LEVEL_CAT11_COND: String? = null, @XmlElement val CODE_CAT11_SELECT: String? = null,
			@XmlElement val CODE_CAT11_FROM: String? = null, @XmlElement val CODE_CAT11_TO: String? = null,
			@XmlElement val LEVEL_CAT12_COND: String? = null, @XmlElement val CODE_CAT12_SELECT: String? = null,
			@XmlElement val CODE_CAT12_FROM: String? = null, @XmlElement val CODE_CAT12_TO: String? = null,
			@XmlElement val LEVEL_CAT13_COND: String? = null, @XmlElement val CODE_CAT13_SELECT: String? = null,
			@XmlElement val CODE_CAT13_FROM: String? = null, @XmlElement val CODE_CAT13_TO: String? = null,
			@XmlElement val LEVEL_CAT14_COND: String? = null, @XmlElement val CODE_CAT14_SELECT: String? = null,
			@XmlElement val CODE_CAT14_FROM: String? = null, @XmlElement val CODE_CAT14_TO: String? = null,
			@XmlElement val LEVEL_CAT15_COND: String? = null, @XmlElement val CODE_CAT15_SELECT: String? = null,
			@XmlElement val CODE_CAT15_FROM: String? = null, @XmlElement val CODE_CAT15_TO: String? = null,
		)
	}

	@Serializable
	data class STATISTICAL_DATA(
		val resultInf: RESULT_INF, val TABLE_INF: TableInf,
		@XmlChildrenName("CLASS_OBJ") val CLASS_INF: List<CLASS_OBJ>, val dataInf: DATA_INF? = null
	){
		val idClassObjMap = buildMap {
			CLASS_INF.forEach {put(it.id, it.codeNameMap)}
		}
		@Serializable
		data class RESULT_INF(
			@XmlElement val TOTAL_NUMBER: Int, @XmlElement val FROM_NUMBER: Int = 0, @XmlElement val TO_NUMBER: Int = 0,
			@XmlElement val NEXT_KEY: Int? = null
		)
		@Serializable
		data class DATA_INF(
			@SerialName("NOTE") val Notes: List<NOTE>, @SerialName("ANNOTATION") val Annotations: List<ANNOTATION>,
			@SerialName("VALUE") val Values: List<VALUE>
		){
			@Serializable
			data class NOTE(
				val char: String, @XmlValue val value: String
			){
				override fun toString(): String {
					return "\"$char\"=\"$value\""
				}
			}
			@Serializable
			data class ANNOTATION(
				val annotation: String, @XmlValue val value: String
			){
				override fun toString(): String {
					return "\"<$annotation>\"=\"$value\""
				}
			}
			@Serializable
			data class VALUE(
				val tab: String? = null, val cat01: String? = null, val cat02: String? = null, val cat03: String? = null,
				val cat04: String? = null, val cat05: String? = null, val cat06: String? = null, val cat07: String? = null,
				val cat08: String? = null, val cat09: String? = null, val cat10: String? = null, val cat11: String? = null,
				val cat12: String? = null, val cat13: String? = null, val cat14: String? = null, val cat15: String? = null,
				val area: String? = null, val time: String? = null, val unit: String? = null, val annotation: String? = null,
				@XmlValue val value: String
			){
				fun toMap(): Map<String, String> {
					return buildMap() {
						tab?.let {put("tab", it)}
						time?.let {put("time", it)}
						area?.let {put("area", it)}
						cat01?.let {put("cat01", it)}
						cat02?.let {put("cat02", it)}
						cat03?.let {put("cat03", it)}
						cat04?.let {put("cat04", it)}
						cat05?.let {put("cat05", it)}
						cat06?.let {put("cat06", it)}
						cat07?.let {put("cat07", it)}
						cat08?.let {put("cat08", it)}
						cat09?.let {put("cat09", it)}
						cat10?.let {put("cat10", it)}
						cat11?.let {put("cat11", it)}
						cat12?.let {put("cat12", it)}
						cat13?.let {put("cat13", it)}
						cat14?.let {put("cat14", it)}
						cat15?.let {put("cat15", it)}
						put("value", "$value${annotation?.let{" <$it>"} ?: ""}")
					}
				}
			}
		}
	}
}