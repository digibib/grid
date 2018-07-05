package com.gu.mediaservice.lib.aws

import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.amazonaws.client.builder.AwsClientBuilder
import com.gu.mediaservice.lib.config.CommonConfig
import play.api.Logger
import play.api.libs.json.{JsValue, Json}


class SNS(config: CommonConfig, topicArn: String) {
  /* DEICHMAN MOD - allow local use
  lazy val client: AmazonSNS = config.withAWSCredentials(AmazonSNSClientBuilder.standard()).build()
  */
   lazy val client: AmazonSNS = config.withAWSCredentials(AmazonSNSClientBuilder.standard())
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://minio", "deichman"))
    .build()

  def publish(message: JsValue, subject: String) {
    val result = client.publish(new PublishRequest(topicArn, Json.stringify(message), subject))
    Logger.info(s"Published message: $result")
  }

}
