package nl.tudelft.ipv8.messaging

import nl.tudelft.ipv8.util.toHexString
import org.junit.Test

import org.junit.Assert.*

data class TestSerializable(private val value: Boolean): Serializable {
    override fun serialize(): ByteArray {
        return serializeBool(value)
    }

    companion object : Deserializable<TestSerializable> {
        override fun deserialize(buffer: ByteArray, offset: Int): TestSerializable {
            return TestSerializable(
                deserializeBool(buffer, 0)
            )
        }
    }
}

class SerializationTest {
    @Test
    fun simplePayload() {
        val serializable = TestSerializable(true)
        val bytes = serializable.serialize()
        val deserialized = TestSerializable.deserialize(bytes)
        assertEquals(1, bytes.size)
        assertEquals(1, bytes[0].toInt())
        assertEquals(serializable, deserialized)
    }

    @Test
    fun serializeBool_true() {
        val serialized = serializeBool(true)
        assertEquals("01", serialized.toHexString())
    }

    @Test
    fun serializeBool_false() {
        val serialized = serializeBool(false)
        assertEquals("00", serialized.toHexString())
    }

    @Test
    fun deserializeBool_true() {
        val serialized = serializeBool(true)
        assertEquals(true, deserializeBool(serialized))
    }

    @Test
    fun deserializeBool_false() {
        val serialized = serializeBool(false)
        assertEquals(false, deserializeBool(serialized))
    }

    @Test
    fun serializeUShort() {
        val serialized = serializeUShort(1025)
        assertEquals("0401", serialized.toHexString())
    }

    @Test
    fun deserializeUShort_simple() {
        val value = 1025
        val serialized = serializeUShort(value)
        assertEquals(value, deserializeUShort(serialized))
    }

    @Test
    fun deserializeUShort_negative() {
        val value = 389
        val serialized = serializeUShort(value)
        assertEquals(value, deserializeUShort(serialized))
    }

    @Test
    fun serializeULong_max() {
        val serialized = serializeULong(18446744073709551615uL)
        assertEquals("ffffffffffffffff", serialized.toHexString())
    }

    @Test
    fun deserializeULong_test() {
        val value = 18446744073709551615uL
        val serialized = serializeULong(value)
        assertEquals(value, deserializeULong(serialized, 0))
    }
}
