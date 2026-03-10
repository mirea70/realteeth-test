package com.realteeth.port.out;

public interface DataSerializerOutPort {
    <T> T deserialize(String data, Class<T> clazz);
    <T> T deserialize(Object data, Class<T> clazz);
    String serialize(Object object);
}
