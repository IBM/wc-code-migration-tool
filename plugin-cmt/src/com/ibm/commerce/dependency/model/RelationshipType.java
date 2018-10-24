package com.ibm.commerce.dependency.model;

/*
 *-----------------------------------------------------------------
 * Copyright 2018 Trent Hoeppner
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
 *-----------------------------------------------------------------
 */

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.commerce.cmt.Check;

/**
 * This enumeration describes the types of relationships that can exist between
 * two items.
 * 
 * @author Trent Hoeppner
 */
public enum RelationshipType {
	CHILD {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			JavaItem target = index.getItem(relationship.getTargetID());
			source.getChildrenIDs().add(target.getID());
			target.setParentID(source.getID());
		}

	},

	DEPENDENCY {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			JavaItem target = index.getItem(relationship.getTargetID());
			source.getDependenciesIDs().add(target.getID());
			target.getIncomingIDs().add(source.getID());
		}

	},

	ARRAY_BASE_CLASS {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			source.setAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS, relationship.getTargetID());
		}

	},

	SUPERCLASS {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			source.setAttribute(JavaItem.ATTR_SUPERCLASS, relationship.getTargetID());
		}

	},

	SUPERINTERFACE {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			Set<Integer> superInterfaces = source.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
			if (superInterfaces == null) {
				superInterfaces = new LinkedHashSet<>();
				source.setAttribute(JavaItem.ATTR_SUPERINTERFACES, superInterfaces);
			}
			superInterfaces.add(relationship.getTargetID());
		}

	},

	METHOD_RETURN_TYPE {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			source.setAttribute(JavaItem.ATTR_RETURN_TYPE, relationship.getTargetID());
		}

	},

	METHOD_PARAMETER_TYPE {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			List<Integer> paramTypes = source.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
			if (paramTypes == null) {
				paramTypes = new ArrayList<>();
				source.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, paramTypes);
			}

			paramTypes.add(relationship.getTargetID());
		}

	},

	FIELD_TYPE {

		@Override
		public void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship) {
			JavaItem source = index.getItem(relationship.getSourceID());
			source.setAttribute(JavaItem.ATTR_FIELD_TYPE, relationship.getTargetID());
		}

	};

	public abstract void setRelationshipOnObjects(JavaItemIndex index, Relationship relationship);

	public Relationship create(JavaItem source, Integer targetID) {
		Check.notNull(source, "source");
		Check.notNull(targetID, "targetID");
		Relationship r = new Relationship();
		r.setSourceID(source.getID());
		r.setTargetID(targetID);
		r.setType(this);

		return r;
	}
}
