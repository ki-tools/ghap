package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ConstraintTypeIdResolver implements TypeIdResolver {

	private JavaType baseType;

	@Override
	public void init(JavaType baseType) {
		this.baseType = baseType;
	}

	@Override
	public String idFromValue(Object value) {
		return idFromValueAndType(value, null);
	}

	@Override
	public String idFromValueAndType(Object value, Class<?> suggestedType) {
		return ((Constraint) value).getType().name();
	}

	@Override
	public JavaType typeFromId(String id) {
		Constraint.ConstraintType constraintType = Constraint.ConstraintType.fromName(id);
		switch (constraintType) {
			case DATE_RANGE:
				return TypeFactory.defaultInstance().constructSpecializedType(baseType, DateRangeConstraint.class);
			case EXCLUDE_DATA_CURATOR:
				return TypeFactory.defaultInstance().constructSpecializedType(baseType, ExcludeDataCuratorsConstraint.class);
		}
		return null;
	}

	@Override
	public JsonTypeInfo.Id getMechanism() {
		return JsonTypeInfo.Id.CUSTOM;
	}
}
