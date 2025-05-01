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

val ESTAT_API_KEY: String = System.getenv("ESTAT_API_KEY")

// Extension function to fetch a list of table
suspend fun HttpClient.getTables(params: Map<String, String>): Pair<Boolean, List<String>> {
	// URI for tables
	val uri = "/rest/3.0/app/getStatsList"
	// Request the list of table from the API with the parameters
	val tables = this.get(uri) {
		parameter("appId", ESTAT_API_KEY)
		params.forEach { param ->
			parameter(param.key, param.value)
		}
		parameter("explanationGetFlg", "N")
	}.body<StatsList>()

	var response = if (tables.result.STATUS != 0 || tables.dataListInf == null) {
		// Return the error message if an error occurred
		listOf(tables.result.ERROR_MSG)
	}
	else {
		// Map each table information to a formatted string
		if (tables.parameter.LANG == "J") {
			listOf("統計表の一覧(statsDataId: 統計表ID, no: 統計表番号, title: 統計表題, surveyName: 調査名, ministry: 所管府省)\n\n" +
					tables.dataListInf.Tables.joinToString("\n") { it.toString() })
		}
		else {
			listOf("List of table\n\n" + tables.dataListInf.Tables.joinToString("\n") { it.toString() })
		}
	}

	if (tables.dataListInf?.resultInf?.NEXT_KEY != null) {
		response = response.plus("...data truncated. The request with 'startPosition = ${tables.dataListInf.resultInf.NEXT_KEY}' will receive the rest.")
	}
	return Pair(tables.result.STATUS != 0, response)
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
		val number: NUMBER, val resultInf: RESULT_INF, @SerialName("TABLE_INF") val Tables: List<TableInf>
	){
		@Serializable
		data class NUMBER(@XmlValue val value: Int)
		@Serializable
		data class RESULT_INF(
			@XmlElement val FROM_NUMBER: Int = 0, @XmlElement val TO_NUMBER: Int = 0, @XmlElement val NEXT_KEY: Int? = null
		)
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
	@XmlElement val UPDATED_DATE: String, val statisticsNameSpec: STATISTICS_NAME_SPEC, val titleSpec: TITLE_SPEC
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
	data class TITLE_SPEC(
		@XmlElement val TABLE_CATEGORY: String? = null, @XmlElement val TABLE_NAME: String,
		@XmlElement val TABLE_SUB_CATEGORY1: String? = null,
		@XmlElement val TABLE_SUB_CATEGORY2: String? = null,
		@XmlElement val TABLE_SUB_CATEGORY3: String? = null
	)

	override fun toString(): String {
		return buildList(){
			add("statsDataId: $id")
			if(title.no != null) add("no: ${title.no}")
			add("title: \"${title.value}\"")
			add("surveyName: \"$STATISTICS_NAME\"")
			add("ministry: \"${govOrg.value}\"")
		}.toString()
	}
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
	) {
		override fun toString(): String {
			return buildList(){
				add("code: \"$code\"")
				add("name: \"$name\"")
				add("level: $level")
				if(unit != null) add("unit: \"$unit\"")
				if(parentCode != null) add("parentCode: \"$parentCode\"")
			}.toString()
		}
	}

	override fun toString(): String {
		return "metaDataID: $id, metaDataName: \"$name\"\n${classes.joinToString("\n") { it.toString() }}\n"
	}
}

// Extension function to fetch data of statistical table
suspend fun HttpClient.getData(params: Map<String, String>): Pair<Boolean, List<String>> {
	// URI for Data
	val uri = "/rest/3.0/app/getStatsData"
	// Request the statistical data from the API with the parameters
	val statsData = this.get(uri) {
		parameter("appId", ESTAT_API_KEY)
		parameter("explanationGetFlg", "N")
		params.forEach { param ->
			parameter(param.key, param.value)
		}
	}.body<StatsDataInfo>()

	var response =  if (statsData.result.STATUS != 0) {
		// Return the error message if an error occurred
		listOf(statsData.result.ERROR_MSG)
	}
	else {
		if (statsData.statisticalData?.dataInf == null) {
			listOf("No data found.")
		}
		else {
			buildList(){
				statsData.statisticalData.dataInf.let {
					add("統計表の値(value: 値, tab: 表章事項符号, time: 調査時期符号, area: 調査地域, unit: 単位, cat01--cat15: 分類事項符号1--15, annotation: 注記符号)\n\n" +
							it.Values.joinToString("\n") { v -> v.toString() })
					add("統計表のメタ情報(code: 符号, name: 名称, level: 階層, unit: 単位, parentCode: 親階層の符号)\n\n" +
							statsData.statisticalData.CLASS_INF.joinToString("\n") { v -> v.toString() })
					if (it.Notes.isNotEmpty()){
						add("値の説明(char: 値, description: この値についての説明)\n\n${it.Notes.joinToString("\n") { v -> v.toString() }}")
					}
					if (it.Annotations.isNotEmpty()){
						add("注記の説明(annotation: 注記符号, description: この注記の内容)\n\n${it.Annotations.joinToString("\n") { v -> v.toString() }}")
					}
				}
			}
		}
	}

	if (statsData.statisticalData?.resultInf?.NEXT_KEY != null) {
		response = response.plus("...data truncated. The request with 'startPosition = ${statsData.statisticalData.resultInf.NEXT_KEY}' will receive the rest.")
	}
	return Pair(statsData.result.STATUS != 0, response)
}

// Data class representing the Metadata response from the API
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
		@Serializable
		data class RESULT_INF(
			@XmlElement val TOTAL_NUMBER: Int, @XmlElement val FROM_NUMBER: Int, @XmlElement val TO_NUMBER: Int,
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
					return "[char: $char, description: \"$value\"]"
				}
			}
			@Serializable
			data class ANNOTATION(
				val annotation: String, @XmlValue val value: String
			){
				override fun toString(): String {
					return "[annotation: \"$annotation\", description: \"$value\"]"
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
				override fun toString(): String {
					return buildList(){
						add("value: $value")
						if(tab != null) add("tab: $tab")
						if(cat01 != null) add("cat01: $cat01")
						if(cat02 != null) add("cat02: $cat02")
						if(cat03 != null) add("cat03: $cat03")
						if(cat04 != null) add("cat04: $cat04")
						if(cat05 != null) add("cat05: $cat05")
						if(cat06 != null) add("cat06: $cat06")
						if(cat07 != null) add("cat07: $cat07")
						if(cat08 != null) add("cat08: $cat08")
						if(cat09 != null) add("cat09: $cat09")
						if(cat10 != null) add("cat10: $cat10")
						if(cat11 != null) add("cat11: $cat11")
						if(cat12 != null) add("cat12: $cat12")
						if(cat13 != null) add("cat13: $cat13")
						if(cat14 != null) add("cat14: $cat14")
						if(cat15 != null) add("cat15: $cat15")
						if(area != null) add("area: $area")
						if(time != null) add("time: $time")
						if(unit != null) add("unit: \"$unit\"")
						if(annotation != null) add("annotation: \"$annotation\"")
					}.toString()
				}
			}
		}
	}
}