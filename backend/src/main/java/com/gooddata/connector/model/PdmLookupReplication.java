/*
 * Copyright (c) 2009, GoodData Corporation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and
 *        the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 *        and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of the GoodData Corporation nor the names of its contributors may be used to endorse
 *        or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gooddata.connector.model;

/**
 * PdmLookupReplication holds the data for replication of the lookup tables.
 * User can setup a reference from a schema column from another schema. The reference is processed as a regular
 * attribute with just one exception. The attribute's lookup is populated with the referenced schema column's lookup
 * rows before the normalization. This makes sure that the column values are translated to the consistent IDs for both
 * schema columns.
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class PdmLookupReplication {

    // the referenced PDM lookup table
    private String referencedLookup;
    // the referencing PDM lookup table
    private String referencingLookup;
    // the referenced PDM lookup table's name (value) column
    private String referencedColumn;
    // the referencing PDM lookup table's name (value) column
    private String referencingColumn;


    /**
     * Constructor
     * @param referencedLookup referenced lookup
     * @param referencedColumn referenced lookup's name/value (not id) column
     * @param referencingLookup referencing lookup
     * @param referencingColumn referencing lookup's name/value (not id) column
     */
    public PdmLookupReplication(String referencedLookup, String referencedColumn, String referencingLookup,
                                String referencingColumn) {
        setReferencedLookup(referencedLookup);
        setReferencingLookup(referencingLookup);
        setReferencedColumn(referencedColumn);
        setReferencingColumn(referencingColumn);
    }

    /**
     * Referenced lookup table name getter
     * @return referenced lookup table name
     */
    public String getReferencedLookup() {
        return referencedLookup;
    }

    /**
     * Referenced lookup table name setter
     * @param referencedLookup referenced lookup table name
     */
    public void setReferencedLookup(String referencedLookup) {
        this.referencedLookup = referencedLookup;
    }

    /**
     * Referencing lookup table name getter
     * @return referencing lookup table name
     */
    public String getReferencingLookup() {
        return referencingLookup;
    }

    /**
     * Referencing lookup table name setter
     * @param referencingLookup referencing lookup table name
     */
    public void setReferencingLookup(String referencingLookup) {
        this.referencingLookup = referencingLookup;
    }

    /**
     * Referenced (name / value not the id) column getter
     * @return referenced (name / value not the id) column
     */
    public String getReferencedColumn() {
        return referencedColumn;
    }

    /**
     * Referenced (name / value not the id) column setter
     * @param referencedColumn Referenced (name / value not the id) column setter
     */
    public void setReferencedColumn(String referencedColumn) {
        this.referencedColumn = referencedColumn;
    }
    /**
     * Referencing (name / value not the id) column getter
     * @return referencing (name / value not the id) column
     */
    public String getReferencingColumn() {
        return referencingColumn;
    }

    /**
     * Referencing (name / value not the id) column setter
     * @param referencingColumn Referencing (name / value not the id) column setter
     */
    public void setReferencingColumn(String referencingColumn) {
        this.referencingColumn = referencingColumn;
    }
}