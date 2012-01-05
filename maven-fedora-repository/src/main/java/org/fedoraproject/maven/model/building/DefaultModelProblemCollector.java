package org.fedoraproject.maven.model.building;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.io.ModelParseException;

/**
 * Collects problems that are encountered during model building. The primary purpose of this component is to account for
 * the fact that the problem reporter has/should not have information about the calling context and hence cannot provide
 * an expressive source hint for the model problem. Instead, the source hint is configured by the model builder before
 * it delegates to other components that potentially encounter problems. Then, the problem reporter can focus on
 * providing a simple error message, leaving the donkey work of creating a nice model problem to this component.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultModelProblemCollector
    implements ModelProblemCollector
{

    private List<ModelProblem> problems;

    private String source;

    private Model sourceModel;

    private Model rootModel;

    private Set<Severity> severities = EnumSet.noneOf( Severity.class );

    public DefaultModelProblemCollector(List<ModelProblem> problems)
    {
        this.problems = ( problems != null ) ? problems : new ArrayList<ModelProblem>();

        for ( ModelProblem problem : this.problems )
        {
            severities.add( problem.getSeverity() );
        }
    }

    public boolean hasFatalErrors()
    {
        return severities.contains( Severity.FATAL );
    }

    public boolean hasErrors()
    {
        return severities.contains( Severity.ERROR ) || severities.contains( Severity.FATAL );
    }

    public List<ModelProblem> getProblems()
    {
        return problems;
    }

    public void setSource( String source )
    {
        this.source = source;
        this.sourceModel = null;
    }

    public void setSource( Model source )
    {
        this.sourceModel = source;
        this.source = null;

        if ( rootModel == null )
        {
            rootModel = source;
        }
    }

    private String getSource()
    {
        if ( source == null && sourceModel != null )
        {
            source = ModelProblemUtils.toPath( sourceModel );
        }
        return source;
    }

    private String getModelId()
    {
        return ModelProblemUtils.toId( sourceModel );
    }

    public void setRootModel( Model rootModel )
    {
        this.rootModel = rootModel;
    }

    public Model getRootModel()
    {
        return rootModel;
    }

    public String getRootModelId()
    {
        return ModelProblemUtils.toId( rootModel );
    }

    public void add( ModelProblem problem )
    {
        problems.add( problem );

        severities.add( problem.getSeverity() );
    }

    public void addAll( List<ModelProblem> problems )
    {
        this.problems.addAll( problems );

        for ( ModelProblem problem : problems )
        {
            severities.add( problem.getSeverity() );
        }
    }

    public void add( Severity severity, String message, InputLocation location, Exception cause )
    {
        int line = -1;
        int column = -1;
        String source = null;
        String modelId = null;

        if ( location != null )
        {
            line = location.getLineNumber();
            column = location.getColumnNumber();
            if ( location.getSource() != null )
            {
                modelId = location.getSource().getModelId();
                source = location.getSource().getLocation();
            }
        }

        if ( modelId == null )
        {
            modelId = getModelId();
            source = getSource();
        }

        if ( line <= 0 && column <= 0 && cause instanceof ModelParseException )
        {
            ModelParseException e = (ModelParseException) cause;
            line = e.getLineNumber();
            column = e.getColumnNumber();
        }

        ModelProblem problem = new DefaultModelProblem( message, severity, source, line, column, modelId, cause );

        add( problem );
    }

}
