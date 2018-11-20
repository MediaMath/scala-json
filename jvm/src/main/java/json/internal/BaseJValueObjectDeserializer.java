/*
 * Copyright 2016 MediaMath, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package json.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import json.*;

import java.io.IOException;

/** Base class used for deserializer. Base done in java for optimized big switch */
public abstract class BaseJValueObjectDeserializer extends StdDeserializer<JValue> {
    abstract protected void throwException(String msg);

    abstract protected JValue parseArray(JsonParser jp, DeserializationContext ctx);

    abstract protected JValue parseObject(JsonParser jp, DeserializationContext ctx);

    abstract protected JValue jString(String str);

    protected BaseJValueObjectDeserializer() {
        super(JValue.class);
    }

    public JValue deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        switch(jp.getCurrentToken()) {
            case START_ARRAY:
                return parseArray(jp, ctx);
            case START_OBJECT:
                return parseObject(jp, ctx);
            case VALUE_STRING:
                return jString(jp.getText());
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return JNumber$.MODULE$.apply(_parseDoublePrimitive(jp, ctx));
            case VALUE_TRUE:
                return JTrue$.MODULE$;
            case VALUE_FALSE:
                return JFalse$.MODULE$;
            case VALUE_NULL:
                return JNull$.MODULE$;
            case VALUE_EMBEDDED_OBJECT:
                throwException("JSON parser - unexpected embedded object");
                return null;
            case END_OBJECT:
                throwException("JSON parser - unexpected end of object");
                return null;
            case FIELD_NAME:
                throwException("JSON parser - unexpected field name");
                return null;
            case END_ARRAY:
                throwException("JSON parser - unexpected end of array");
                return null;
            case NOT_AVAILABLE:
                throwException("JSON parser - unexpected end of token");
                return null;
            default:
                throwException("JSON parser - unexpected token " + jp.getCurrentToken());
                return null;
        }
    }
}
