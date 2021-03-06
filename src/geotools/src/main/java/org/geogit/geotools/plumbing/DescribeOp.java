/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;

/**
 * Internal operation for describing a table from a GeoTools {@link DataStore}.
 * 
 * @see DataStore
 */
public class DescribeOp extends AbstractGeoGitOp<Optional<Map<String, String>>> {

    private String table = null;

    private DataStore dataStore;

    /**
     * Describes a table from the data store that has been assigned.
     * 
     * @return a map that contains all properties and their types from the provided table
     */
    @Override
    public Optional<Map<String, String>> call() {
        if (dataStore == null) {
            throw new GeoToolsOpException(StatusCode.DATASTORE_NOT_DEFINED);
        }
        if (table == null || table.isEmpty()) {
            throw new GeoToolsOpException(StatusCode.TABLE_NOT_DEFINED);
        }

        Map<String, String> propertyMap = new HashMap<String, String>();

        boolean foundTable = false;

        List<Name> typeNames;
        try {
            typeNames = dataStore.getNames();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
        }

        for (Name typeName : typeNames) {
            if (!table.equals(typeName.toString()))
                continue;

            foundTable = true;

            SimpleFeatureSource featureSource;
            try {
                featureSource = dataStore.getFeatureSource(typeName);
            } catch (Exception e) {
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_FEATURES);
            }

            SimpleFeatureType featureType = featureSource.getSchema();

            Collection<PropertyDescriptor> descriptors = featureType.getDescriptors();
            for (PropertyDescriptor descriptor : descriptors) {
                propertyMap.put(descriptor.getName().toString(), descriptor.getType().getBinding()
                        .getSimpleName());
            }
        }

        if (!foundTable) {
            return Optional.absent();
        }
        return Optional.of(propertyMap);
    }

    /**
     * @param table the table to describe
     * @return {@code this}
     */
    public DescribeOp setTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * @param dataStore the data store that contains the table to describe
     * @return {@code this}
     * @see DataStore
     */
    public DescribeOp setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }
}
