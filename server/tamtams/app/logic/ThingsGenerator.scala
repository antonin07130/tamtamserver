package logic

import play.api.Logger

/**
  * Created by antoninpa on 7/29/16.
  */
object ThingsGenerator {
  val testThingString1 : String =
    "\"id\":\"TELEPHONEID000000023\"," +
    "\"pict\":\"8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1\"," +
    "\"desc\":\"un truc a vendre\"," +
    "\"price\":{" +
      "\"currency\":\"978\"," +
      "\"price\":\"10.50\"" +
      "}," +
    "\"location\":{" +
      "\"lat\":\"25.22\"," +
      "\"lon\":\"109.55\"" +
      "}," +
    "\"stuck\":\"False\"}"

  Logger.info(s"tamtams : testThingString1 created  : $testThingString1 .")

}
