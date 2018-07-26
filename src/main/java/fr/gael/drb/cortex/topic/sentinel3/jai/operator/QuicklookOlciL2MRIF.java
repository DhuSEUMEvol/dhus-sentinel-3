package fr.gael.drb.cortex.topic.sentinel3.jai.operator;

import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.query.Query;
import fr.gael.drb.value.Integer;
import fr.gael.drb.value.UnsignedLong;
import fr.gael.drb.value.Long;
import fr.gael.drb.value.Value;
import fr.gael.drb.value.ValueArray;
import fr.gael.drbx.image.DrbCollectionImage;
import fr.gael.drbx.image.DrbImage;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayLong;
import ucar.ma2.ArrayLong.D2;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;

/**
 * Created by vinsan on 30/10/17.
 */
public class QuicklookOlciL2MRIF implements RenderedImageFactory
{
    private static final Logger LOGGER = Logger.getLogger(QuicklookOlciL2MRIF.class);

    /**
     * it uses the clorophyll index plus mask to fill in empty areas
     * clorophyll index is mapped using a color map
     * @param param the clorophyll index
     * @param hints Optionally contains destination image layout.
     */
    @Override
    public RenderedImage create(ParameterBlock param, RenderingHints hints)
    {
    	
    	Common.PixelCorrection pc = ((Common.PixelCorrection[])param.getObjectParameter(0))[0];

        int[][] flags = (int[][]) param.getObjectParameter(1);

        RenderedImage raw = (RenderedImage) param.getSource(0);

        int width = raw.getData().getWidth();
        int height = raw.getData().getHeight();

        double[][] var = new double[width][height];

        // sanity checks on input data
        if(pc == null)
            throw new IllegalArgumentException("Pixel corrections can't be null");

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                if (raw.getData().getSample(j, i, 0) != pc.nodata )
                    var[j][i] = raw.getData().getSample(j, i, 0) * pc.scale + pc.offset;

        BufferedImage out = new BufferedImage(var.length, var[0].length, BufferedImage.TYPE_3BYTE_BGR);
        TreeMap<Float, Color> colorMap = Common.loadColormap("clph.cpd",
                -2,
                2);

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                if (isInvalid(flags[i][j]))
                    out.setRGB(j, i, Color.BLACK.getRGB());
                else if(isCloud(flags[i][j]))
                    out.setRGB(j, i, new Color(255, 255, 255).getRGB());
                else if (isSnowIce(flags[i][j]))
                    out.setRGB(j, i, new Color(185, 253, 255).getRGB());
                else if (isLand(flags[i][j]))
                    out.setRGB(j, i, new Color(117,73,9).getRGB());
                else if (raw.getData().getSample(j, i, 0) != pc.nodata)
                    out.setRGB(j, i, Common.colorMap((float) var[j][i], colorMap).getRGB());
                else
                    out.setRGB(j, i, Color.WHITE.getRGB());
            }

        return out;
    }

    public static int[][] extractFlags(DrbCollectionImage sources)
    {
        try
        {
            DrbImage image = sources.getChildren().iterator().next();
            DrbNode node = ((DrbNode) (image.getItemSource()));
            String path = node.getPath();
//to be removed
            Query query_rows_number = new Query(
                    "wqsf.nc/root/dimensions/rows");
            Query query_cols_number = new Query(
                    "wqsf.nc/root/dimensions/columns");

         // get path of the product that is injected           
            String name = node.getName();

// get the file name of the product
            String pathProd = path;
//manage the zip file in case imported via scanner                       
            if (path.toLowerCase().contains("zip")) {
            	String pathZIP = path.substring(0, path.lastIndexOf("/") );
            	pathProd = pathZIP.substring(0, pathZIP.lastIndexOf("/") );
          
            	FileOutputStream fileoutputstream;
            	ZipInputStream zip = new ZipInputStream(new FileInputStream(pathZIP));
            	ZipEntry zipEntry = null;
            	zipEntry = zip.getNextEntry( );
            	byte[] buf = new byte[1024];
            	int n;
            	while( ( zipEntry = zip.getNextEntry( ) ) != null )
            	{
            		if( zipEntry.getName( ).equals( name + "/wqsf.nc" ) )
            		{ 
            			fileoutputstream = new FileOutputStream(pathProd +"/wqsf.nc" );
            			while ((n = zip.read(buf, 0, 1024)) > -1) {
            				fileoutputstream.write(buf, 0, n);
            			}
            			fileoutputstream.close();
            		}
            	}
            	zip.close();
            }
            
            NetcdfDataset dswqsf = NetcdfDataset.openDataset(pathProd + "/wqsf.nc");
            Variable data = dswqsf.findVariable("WQSF");
            ArrayLong.D2 dataArray = (D2) data.read();
            
            Value vrows = query_rows_number.evaluate(node).getItem(0).getValue().
                    convertTo(Value.INTEGER_ID);
            Value vcols = query_cols_number.evaluate(node).getItem(0).getValue().
                    convertTo(Value.INTEGER_ID);

            int rows = ((Integer) vrows).intValue();
            int cols = ((Integer) vcols).intValue();
//            DrbSequence sequence = query_data.evaluate(node);
            
            int[][] ds = new int[rows][cols];
//            int vartmp = 0;
            for (int index_rows = 0; index_rows < rows; index_rows++)
            {
            	for (int index_cols = 0; index_cols < cols; index_cols++)

                {                    
//            		vartmp = (int) (dataArray.get(index_rows, index_cols));
                    ds[index_rows][index_cols] = (int) (dataArray.get(index_rows, index_cols));                    	
                }
            }
            dswqsf.close();
            if (path.toLowerCase().contains("zip")) {
            	boolean success = (new File(pathProd + "/wqsf.nc")).delete();
            }

            return ds;
        }
        catch (Exception e)
        {
            LOGGER.error("Marine flags extraction failure.", e);
            return null;
        }
    }

    
    private boolean isSnowIce(int mask)
    {
        return (mask & 16) > 0;
    }

    private boolean isCloud(int mask)
    {
        return (mask & 8) > 0;
    }

    private boolean isLand(int mask)
    {
        return (mask & 4) > 0;
    }

    private boolean isWater(int mask)
    {
        return (mask & 2) > 0;
    }

    private boolean isInvalid(int mask)
    {
        return (mask & 1) > 0;
    }
}
