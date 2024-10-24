// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.formrecognizer;

import com.azure.ai.formrecognizer.models.FieldValueType;
import com.azure.ai.formrecognizer.models.FormField;
import com.azure.ai.formrecognizer.models.OperationResult;
import com.azure.ai.formrecognizer.models.RecognizedForm;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.PollerFlux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.azure.ai.formrecognizer.implementation.Utility.toFluxByteBuffer;

/**
 * Async sample for recognizing commonly found US receipt fields from a local file input stream.
 * For a suggested approach to extracting information from receipts, see StronglyTypedRecognizedForm.java.
 * See fields found on a receipt here:
 * https://aka.ms/azsdk/python/formrecognizer/receiptfields
 */
public class RecognizeReceiptsAsync {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     *
     * @throws IOException Exception thrown when there is an error in reading all the bytes from the File.
     */
    public static void main(final String[] args) throws IOException {
        // Instantiate a client that will be used to call the service.
        FormRecognizerAsyncClient client = new FormRecognizerClientBuilder()
            .credential(new AzureKeyCredential("{key}"))
            .endpoint("https://{endpoint}.cognitiveservices.azure.com/")
            .buildAsyncClient();

        File sourceFile = new File("../formrecognizer/azure-ai-formrecognizer/src/samples/java/sample-forms/"
            + "receipts/contoso-allinone.jpg");
        byte[] fileContent = Files.readAllBytes(sourceFile.toPath());
        PollerFlux<OperationResult, List<RecognizedForm>> recognizeReceiptPoller;
        try (InputStream targetStream = new ByteArrayInputStream(fileContent)) {
            recognizeReceiptPoller = client.beginRecognizeReceipts(toFluxByteBuffer(targetStream), sourceFile.length());
        }

        Mono<List<RecognizedForm>> receiptPageResultsMono = recognizeReceiptPoller
            .last()
            .flatMap(pollResponse -> {
                if (pollResponse.getStatus().isComplete()) {
                    System.out.println("Polling completed successfully");
                    // training completed successfully, retrieving final result.
                    return pollResponse.getFinalResult();
                } else {
                    return Mono.error(new RuntimeException("Polling completed unsuccessfully with status:"
                        + pollResponse.getStatus()));
                }
            });

        receiptPageResultsMono.subscribe(receiptPageResults -> {
            for (int i = 0; i < receiptPageResults.size(); i++) {
                RecognizedForm recognizedForm = receiptPageResults.get(i);
                Map<String, FormField<?>> recognizedFields = recognizedForm.getFields();
                System.out.printf("----------- Recognized receipt info for page %d -----------%n", i);
                FormField<?> merchantNameField = recognizedFields.get("MerchantName");
                if (merchantNameField != null) {
                    if (FieldValueType.STRING == merchantNameField.getValueType()) {
                        String merchantName = FieldValueType.STRING.cast(merchantNameField);
                        System.out.printf("Merchant Name: %s, confidence: %.2f%n",
                            merchantName, merchantNameField.getConfidence());
                    }
                }

                FormField<?> merchantPhoneNumberField = recognizedFields.get("MerchantPhoneNumber");
                if (merchantPhoneNumberField != null) {
                    if (FieldValueType.PHONE_NUMBER == merchantPhoneNumberField.getValueType()) {
                        String merchantAddress = FieldValueType.PHONE_NUMBER.cast(merchantPhoneNumberField);
                        System.out.printf("Merchant Phone number: %s, confidence: %.2f%n",
                            merchantAddress, merchantPhoneNumberField.getConfidence());
                    }
                }

                FormField<?> merchantAddressField = recognizedFields.get("MerchantAddress");
                if (merchantAddressField != null) {
                    if (FieldValueType.STRING == merchantAddressField.getValueType()) {
                        String merchantAddress = FieldValueType.STRING.cast(merchantAddressField);
                        System.out.printf("Merchant Address: %s, confidence: %.2f%n",
                            merchantAddress, merchantAddressField.getConfidence());
                    }
                }

                FormField<?> transactionDateField = recognizedFields.get("TransactionDate");
                if (transactionDateField != null) {
                    if (FieldValueType.DATE == transactionDateField.getValueType()) {
                        LocalDate transactionDate = FieldValueType.DATE.cast(transactionDateField);
                        System.out.printf("Transaction Date: %s, confidence: %.2f%n",
                            transactionDate, transactionDateField.getConfidence());
                    }
                }

                FormField<?> receiptItemsField = recognizedFields.get("Items");
                if (receiptItemsField != null) {
                    System.out.printf("Receipt Items: %n");
                    if (FieldValueType.LIST == receiptItemsField.getValueType()) {
                        List<FormField<?>> receiptItems = FieldValueType.LIST.cast(receiptItemsField);
                        receiptItems.stream()
                            .filter(receiptItem -> FieldValueType.MAP == receiptItem.getValueType())
                            .<Map<String, FormField<?>>>map(FieldValueType.MAP::cast)
                            .forEach(formFieldMap -> formFieldMap.forEach((key, formField) -> {
                                if ("Name".equals(key)) {
                                    if (FieldValueType.STRING == formField.getValueType()) {
                                        String name = FieldValueType.STRING.cast(formField);
                                        System.out.printf("Name: %s, confidence: %.2fs%n",
                                            name, formField.getConfidence());
                                    }
                                }
                                if ("Quantity".equals(key)) {
                                    if (FieldValueType.DOUBLE == formField.getValueType()) {
                                        Float quantity = FieldValueType.DOUBLE.cast(formField);
                                        System.out.printf("Quantity: %f, confidence: %.2f%n",
                                            quantity, formField.getConfidence());
                                    }
                                }
                                if ("Price".equals(key)) {
                                    if (FieldValueType.DOUBLE == formField.getValueType()) {
                                        Float price = FieldValueType.DOUBLE.cast(formField);
                                        System.out.printf("Price: %f, confidence: %.2f%n",
                                            price, formField.getConfidence());
                                    }
                                }
                                if ("TotalPrice".equals(key)) {
                                    if (FieldValueType.DOUBLE == formField.getValueType()) {
                                        Float totalPrice = FieldValueType.DOUBLE.cast(formField);
                                        System.out.printf("Total Price: %f, confidence: %.2f%n",
                                            totalPrice, formField.getConfidence());
                                    }
                                }
                            }));
                    }
                }
            }
        });

        // The .subscribe() creation and assignment is not a blocking call. For the purpose of this example, we sleep
        // the thread so the program does not end before the send operation is complete. Using .block() instead of
        // .subscribe() will turn this into a synchronous call.
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
