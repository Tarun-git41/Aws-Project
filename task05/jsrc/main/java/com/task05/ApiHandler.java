package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role",
		isPublishVersion = true,
		aliasName = "${lambdas_alias_name}",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private final String TABLE_NAME = System.getenv("target_table"); // Get table name from environment variable
	private final DynamoDB dynamoDB;

	public ApiHandler() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		dynamoDB = new DynamoDB(client);
	}

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
		Map<String, Object> response = new HashMap<>();
		try {
			// Parse request
			int principalId = (int) request.get("principalId");
			Map<String, String> content = (Map<String, String>) request.get("content");

			// Create unique ID and timestamp
			String id = UUID.randomUUID().toString();
			String createdAt = Instant.now().toString();

			// Create DynamoDB item
			Item item = new Item()
					.withPrimaryKey("id", id)
					.withNumber("principalId", principalId)
					.withString("createdAt", createdAt)
					.withMap("body", content);

			// Put item into the DynamoDB table
			Table table = dynamoDB.getTable(TABLE_NAME);
			table.putItem(item);

			// Prepare response
			Map<String, Object> event = new HashMap<>();
			event.put("id", id);
			event.put("principalId", principalId);
			event.put("createdAt", createdAt);
			event.put("body", content);

			response.put("statusCode", 201);
			response.put("event", event);
		} catch (Exception e) {
			context.getLogger().log("Error: " + e.getMessage());
			response.put("statusCode", 500);
			response.put("error", "Internal server error");
		}
		return response;
	}
}
