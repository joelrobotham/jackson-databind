package com.fasterxml.jackson.databind;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Abstract class that defines API used by {@link ObjectMapper} (and
 * other chained {@link JsonSerializer}s too) to serialize Objects of
 * arbitrary types into JSON, using provided {@link JsonGenerator}.
 * {@link com.fasterxml.jackson.databind.ser.std.StdSerializer} instead
 * of this class, since it will implement many of optional
 * methods of this class.
 *<p>
 * NOTE: various <code>serialize</code> methods are never (to be) called
 * with null values -- caller <b>must</b> handle null values, usually
 * by calling {@link SerializerProvider#findNullValueSerializer} to obtain
 * serializer to use.
 * This also means that custom serializers can not be directly used to change
 * the output to produce when serializing null values.
 *<p>
 * If serializer is an aggregate one -- meaning it delegates handling of some
 * of its contents by using other serializer(s) -- it typically also needs
 * to implement {@link com.fasterxml.jackson.databind.ser.ResolvableSerializer},
 * which can locate secondary serializers needed. This is important to allow dynamic
 * overrides of serializers; separate call interface is needed to separate
 * resolution of secondary serializers (which may have cyclic link back
 * to serializer itself, directly or indirectly).
 *<p>
 * In addition, to support per-property annotations (to configure aspects
 * of serialization on per-property basis), serializers may want
 * to implement 
 * {@link com.fasterxml.jackson.databind.ser.ContextualSerializer},
 * which allows specialization of serializers: call to
 * {@link com.fasterxml.jackson.databind.ser.ContextualSerializer#createContextual}
 * is passed information on property, and can create a newly configured
 * serializer for handling that particular property.
 *<p>
 * If both
 * {@link com.fasterxml.jackson.databind.ser.ResolvableSerializer} and
 * {@link com.fasterxml.jackson.databind.ser.ContextualSerializer}
 * are implemented, resolution of serializers occurs before
 * contextualization.
 */
public abstract class JsonSerializer<T>
    implements JsonFormatVisitable // since 2.1
{
    /*
    /**********************************************************
    /* Fluent factory methods for constructing decorated versions
    /**********************************************************
     */

    /**
     * Method that will return serializer instance that produces
     * "unwrapped" serialization, if applicable for type being
     * serialized (which is the case for some serializers
     * that produce JSON Objects as output).
     * If no unwrapped serializer can be constructed, will simply
     * return serializer as-is.
     *<p>
     * Default implementation just returns serializer as-is,
     * indicating that no unwrapped variant exists
     * 
     * @param unwrapper Name transformation to use to convert between names
     *   of unwrapper properties
     */
    public JsonSerializer<T> unwrappingSerializer(NameTransformer unwrapper) {
        return this;
    }

    /**
     * Method that can be called to try to replace serializer this serializer
     * delegates calls to. If not supported (either this serializer does not
     * delegate anything; or it does not want any changes), should either
     * throw {@link UnsupportedOperationException} (if operation does not
     * make sense or is not allowed); or return this serializer as is.
     * 
     * @since 2.1
     */
    public JsonSerializer<T> replaceDelegatee(JsonSerializer<?> delegatee) {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************
    /* Serialization methods
    /**********************************************************
     */

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     *
     * @param value Value to serialize; can <b>not</b> be null.
     * @param gen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     */
    public abstract void serialize(T value, JsonGenerator gen, SerializerProvider provider)
        throws IOException, JsonProcessingException;

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles, using specified type serializer
     * for embedding necessary type information.
     *<p>
     * Default implementation will throw {@link UnsupportedOperationException}
     * to indicate that proper type handling needs to be implemented.
     *<p>
     * For simple datatypes written as a single scalar value (JSON String, Number, Boolean),
     * implementation would look like:
     *<pre>
     *  // note: method to call depends on whether this type is serialized as JSON scalar, object or Array!
     *  typeSer.writeTypePrefixForScalar(value, gen);
     *  serialize(value, gen, provider);
     *  typeSer.writeTypeSuffixForScalar(value, gen);
     *</pre>
     * and implementations for type serialized as JSON Arrays or Objects would differ slightly,
     * as <code>START-ARRAY>/<code>END-ARRAY</code> and
     * <code>START-OBJECT>/<code>END-OBJECT</code> pairs
     * need to be properly handled with respect to serializing of contents.
     *
     * @param value Value to serialize; can <b>not</b> be null.
     * @param gen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     * @param typeSer Type serializer to use for including type information
     */
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        Class<?> clz = handledType();
        if (clz == null) {
            clz = value.getClass();
        }
        throw new UnsupportedOperationException("Type id handling not implemented for type "+clz.getName());
    }
    
    /*
    /**********************************************************
    /* Other accessors
    /**********************************************************
     */
    
    /**
     * Method for accessing type of Objects this serializer can handle.
     * Note that this information is not guaranteed to be exact -- it
     * may be a more generic (super-type) -- but it should not be
     * incorrect (return a non-related type).
     *<p>
     * Default implementation will return null, which essentially means
     * same as returning <code>Object.class</code> would; that is, that
     * nothing is known about handled type.
     *<p>
     */
    public Class<T> handledType() { return null; }

    /**
     * Method called to check whether given serializable value is
     * considered "empty" value (for purposes of suppressing serialization
     * of empty values).
     *<p>
     * Default implementation will consider only null values to be empty.
     * 
     * @since 2.0
     * 
     * @deprecated Since 2.5 Use {@link #isEmpty(SerializerProvider, Object)} instead
     */
    @Deprecated
    public boolean isEmpty(T value) {
        return (value == null);
    }

    /**
     * Method called to check whether given serializable value is
     * considered "empty" value (for purposes of suppressing serialization
     * of empty values).
     *<p>
     * Default implementation will consider only null values to be empty.
     *<p>
     * NOTE: replaces {@link #isEmpty(Object)}, deprecated in 2.5
     * 
     * @since 2.5
     */
    public boolean isEmpty(SerializerProvider provider, T value) {
        // replace with implementation in 2.6 or later
        return isEmpty(value);
    }
    
    /**
     * Method that can be called to see whether this serializer instance
     * will use Object Id to handle cyclic references.
     */
    public boolean usesObjectId() {
        return false;
    }

    /**
     * Accessor for checking whether this serializer is an
     * "unwrapping" serializer; this is necessary to know since
     * it may also require caller to suppress writing of the
     * leading property name.
     */
    public boolean isUnwrappingSerializer() {
        return false;
    }
    
    /**
     * Accessor that can be used to determine if this serializer uses
     * another serializer for actual serialization, by delegating
     * calls. If so, will return immediate delegate (which itself may
     * delegate to further serializers); otherwise will return null.
     * 
     * @return Serializer this serializer delegates calls to, if null;
     *   null otherwise.
     * 
     * @since 2.1
     */
    public JsonSerializer<?> getDelegatee() {
        return null;
    }

    /*
    /**********************************************************
    /* Default JsonFormatVisitable implementation
    /**********************************************************
     */

    /**
     * Default implementation simply calls {@link JsonFormatVisitorWrapper#expectAnyFormat(JavaType)}.
     * 
     * @since 2.1
     */
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type)
        throws JsonMappingException
    {
        if (visitor != null) visitor.expectAnyFormat(type);
    }

    /*
    /**********************************************************
    /* Helper class(es)
    /**********************************************************
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no serializer is configured</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link com.fasterxml.jackson.databind.annotation.JsonSerialize}.
     */
    public abstract static class None
        extends JsonSerializer<Object> { }
}
