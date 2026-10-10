/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.messaging;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer {

    private static final int SCHEMA_ID_SIZE = 4; // Confluent wire format: magic byte (1) + schema ID (4 bytes)
    private static final byte MAGIC_BYTE = 0;

    private AvroSerializer() {
        // Utility class
    }

    public static byte[] serialize(GenericRecord record, Schema schema) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Write Confluent wire format header: magic byte + schema ID
        out.write(MAGIC_BYTE);
        out.write(intToBytes(getSchemaId(schema)));
        
        // Serialize the record
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(record, encoder);
        encoder.flush();
        
        return out.toByteArray();
    }

    public static byte[] serialize(GenericRecord record, Schema schema, int schemaId) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Write Confluent wire format header
        out.write(MAGIC_BYTE);
        out.write(intToBytes(schemaId));
        
        // Serialize the record
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(record, encoder);
        encoder.flush();
        
        return out.toByteArray();
    }

    private static int getSchemaId(Schema schema) {
        // In production, this would come from Schema Registry
        // For local development, we use a hash of the schema
        return Math.abs(schema.toString().hashCode());
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }
}