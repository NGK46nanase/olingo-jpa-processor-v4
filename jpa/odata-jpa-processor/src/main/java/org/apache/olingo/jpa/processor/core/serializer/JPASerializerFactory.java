package org.apache.olingo.jpa.processor.core.serializer;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.api.JPASerializer;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

public class JPASerializerFactory {
  private final ServiceMetadata serviceMetadata;
  private final OData odata;
  private UriHelper uriHelper;

  public JPASerializerFactory(final OData odata, final ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
    this.uriHelper = odata.createUriHelper();
  };

  public JPASerializer createSerializer(final ContentType responseFormat, final UriInfo uriInfo)
      throws ODataApplicationException, SerializerException {
    // Assumption: Type of last resource path item rules the type of the response
    List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    UriResource lastItem = resourceParts.get(resourceParts.size() - 1);

    switch (lastItem.getKind()) {
    case entitySet:
    case navigationProperty:
      if (((UriResourcePartTyped) lastItem).isCollection())
        return new JPASerializeCollection(serviceMetadata, odata.createSerializer(responseFormat), uriHelper, uriInfo);
      else
        return new JPASerializeEntity(serviceMetadata, odata.createSerializer(responseFormat), uriHelper, uriInfo);
    case complexProperty:
      return new JPASerializeComplex(serviceMetadata, odata.createSerializer(responseFormat), uriHelper, uriInfo);
    case primitiveProperty:
      // TODO Check if this is correct. Wouldn't it make sense to always call "serializePrimitiveResult"
      if (resourceParts.size() > 1 && resourceParts.get(resourceParts.size() - 2) instanceof UriResourceComplexProperty)
        return new JPASerializeComplex(serviceMetadata, odata.createSerializer(responseFormat), uriHelper, uriInfo);
      else
        return new JPASerializePrimitive(serviceMetadata, odata.createSerializer(responseFormat), uriHelper, uriInfo);
    case function:
      return new JPASerializeFunction(serviceMetadata, odata.createSerializer(responseFormat), uriHelper, uriInfo);
    case count:
      return new JPASerializeCount();
    default:
      // TODO error handling
      throw new ODataApplicationException("Resource type " + lastItem.getKind() + " not supported",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }
  }
}
