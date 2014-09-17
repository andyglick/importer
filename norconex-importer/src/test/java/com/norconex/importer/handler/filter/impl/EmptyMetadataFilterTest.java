/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

public class EmptyMetadataFilterTest {

    @Test
    public void testAcceptDocument() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "a string to match");
        meta.addString("field2", "");

        EmptyMetadataFilter filter = new EmptyMetadataFilter();

        filter.setFields("field1");
        filter.setOnMatch(OnMatch.EXCLUDE);
        
        Assert.assertTrue("field1 not filtered properly.", 
                filter.acceptDocument("n/a", null, meta, false));

        filter.setFields("field2");
        Assert.assertFalse("field2 not filtered properly.", 
                filter.acceptDocument("n/a", null, meta, false));

        filter.setFields("field3");
        Assert.assertFalse("field3 not filtered properly.", 
                filter.acceptDocument("n/a", null, meta, false));
    }    
    
    @Test
    public void testWriteRead() throws IOException {
        EmptyMetadataFilter filter = new EmptyMetadataFilter();
        filter.addRestriction("author", "Pascal.*", false);
        filter.setFields("field1", "field2", "field3");
        filter.setOnMatch(OnMatch.INCLUDE);
        System.out.println("Writing/Reading this: " + filter);
        ConfigurationUtil.assertWriteRead(filter);
    }
}
