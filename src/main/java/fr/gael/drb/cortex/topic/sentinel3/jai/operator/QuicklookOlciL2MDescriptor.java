/*
 * Data HUb Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 EUMETSAT
 * Copyright (C) 2013,2014,2015,2016 European Space Agency (ESA)
 * Copyright (C) 2013,2014,2015,2016 GAEL Systems
 * Copyright (C) 2013,2014,2015,2016 Serco Spa
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gael.drb.cortex.topic.sentinel3.jai.operator;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 * Created by vinsan on 31/05/2017.
 */
public class QuicklookOlciL2MDescriptor extends OperationDescriptorImpl {

    /**
     * The "QuicklookOlciL2M" operation name.
     */
    public final static String OPERATION_NAME = "QuicklookOlciL2M";

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "Olci" operation.
     */
    protected static String[][] resources =
            {
                    { "GlobalName", OPERATION_NAME },
                    { "LocalName", OPERATION_NAME },
                    { "Vendor", "fr.gael.drb.cortex.topic.sentinel3.jai.operator" },
                    { "Description", "Performs the rendering of S3 OLCI L2 Marine dataset." },
                    { "DocURL", "http://www.gael.fr/drb" },
                    { "Version", "1.0" },
                    { "arg0Desc", "pixels correction"},
                    { "arg1Desc", "quality flags"}
            };

    /**
     * Modes supported by this operator.
     */
    private static String[] supportedModes = { "rendered" };

    /**
     * The parameter names for the "QuicklookOlci" operation..
     */
    private static String[] paramNames = { "pixels_correction", "flags" };

    /**
     * The parameter class types for the "QuicklookOlci" operation.
     */
    private static Class<?>[] paramClasses = { Common.PixelCorrection[].class, int[][].class };

    /**
     * The parameter default values for the "QuicklookOlci" operation..
     */
    private static Object[] paramDefault={null, null};

    /**
     * Constructs a new Olci operator, with the parameters specified in
     * static fields. 3 sources are expected.
     */
    public QuicklookOlciL2MDescriptor()
    {
        super(resources, supportedModes, 1, paramNames, paramClasses,
                null, null);
    }

    /**
     * Create the Render Operator to compute Olci  L2M quicklook.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String, ParameterBlock, RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param OGVI the RenderedImage
     * @param LQSF the RenderedImage
     * @param pixels_correction per bands scale/offset pixels correction
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if sources is null.
     * @throws IllegalArgumentException if a source is null.
     */
    public static RenderedOp create(Common.PixelCorrection[]pixels_correction, int[][] flags,
                                    RenderingHints hints, RenderedImage... sources)
    {
        ParameterBlockJAI pb =
                new ParameterBlockJAI(OPERATION_NAME,
                        RenderedRegistryMode.MODE_NAME);

        int numSources = sources.length;
        // Check on the source number
        if (numSources <= 0)
        {
            throw new IllegalArgumentException("No resources are present");
        }

        // Setting of all the sources
        for (int index = 0; index < numSources; index++)
        {
            RenderedImage source = sources[index];
            if (source == null)
            {
                throw new IllegalArgumentException("This resource is null");
            }
            pb.setSource(source, index);
        }
      /*To Be remove */
        pb.setParameter(paramNames[0], pixels_correction);
        pb.setParameter(paramNames[1], flags);

        return JAI.create(OPERATION_NAME, pb, hints);
    } //create

}
