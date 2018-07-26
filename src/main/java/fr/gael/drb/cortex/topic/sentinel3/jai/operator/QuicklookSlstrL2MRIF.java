package fr.gael.drb.cortex.topic.sentinel3.jai.operator;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.query.Query;
import fr.gael.drbx.image.DrbCollectionImage;
import fr.gael.drbx.image.DrbImage;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayDouble.D2;
import ucar.ma2.ArrayDouble.D3;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Created by vinsan on 30/10/17.
 */
public class QuicklookSlstrL2MRIF implements RenderedImageFactory
{
    private static final Logger LOGGER = Logger.getLogger(QuicklookSlstrL2MRIF.class);
//    private static Common common;

    @Override
    public RenderedImage create(ParameterBlock param, RenderingHints renderingHints)
    {
 //       Common.PixelCorrection pc = ((Common.PixelCorrection[])param.getObjectParameter(0))[0];

    	int[][] flags = (int[][]) param.getObjectParameter(0);
        double[][] sst = (double[][]) param.getObjectParameter(1);
        
        int width = sst.length;        
        int height = sst[0].length;        
        
        BufferedImage out = new BufferedImage(sst.length, sst[0].length, BufferedImage.TYPE_3BYTE_BGR);
        TreeMap<Float, Color> colorMap =Common.loadColormap("sst.cpd",
                272,
                308.5);
        
        for (int i = 0; i < height; i++) 
            for (int j = 0; j < width; j++)
            {
                 if (sst[j][i] == Double.NaN)
                	 out.setRGB(j, i, Color.BLACK.getRGB());
                 else if (isCloud(flags[j][i]))
                     out.setRGB(j, i, new Color(255, 255, 255).getRGB());
                 else if (isLand(flags[j][i]))
                     out.setRGB(j, i, new Color(117,73,9).getRGB());
                 else if (isSnowIce(flags[j][i]))
                     out.setRGB(j, i, new Color(185, 253, 255).getRGB());
                 else if (isMicro(flags[j][i]))
                     out.setRGB(j, i, new Color(0,0,0).getRGB());
//                 else if (isGlint(flags[j][i]))
//                     out.setRGB(j, i, new Color(0,0,0).getRGB());        
                 else if (isExceptions(flags[j][i]))
                     out.setRGB(j, i, new Color(0, 0, 0).getRGB());
                 else if (isOverflow(flags[j][i]))
                      out.setRGB(j, i, new Color(0, 0, 0).getRGB());
                 else if (isStrato(flags[j][i]))
                     out.setRGB(j, i, new Color(0, 0, 0).getRGB());
                 else
                	 out.setRGB(j, i, Common.colorMap((float) sst[j][i], colorMap).getRGB());
            }

        return out;

    }

    public static double[][] extractL2P(DrbCollectionImage sources)
    {
        try
        {
        	DrbImage image = sources.getChildren().iterator().next();
            DrbNode node = ((DrbNode) (image.getItemSource()));
              
// get path of the product that is injected           
            String path = node.getPath();
            String name = node.getName();

// get the file name of the product
            Query q = new Query("data(xfdumanifest.xml/XFDU/dataObjectSection/"
                    + "dataObject[@ID=\"L2P_Data\"]/byteStream/fileLocation/@href)");
            DrbSequence seq = q.evaluate(node);

            String L2Pnametmp = seq.getItem(0).getValue().toString();            
            String L2Pname = L2Pnametmp.substring(2);
            String pathProd = path;

//manage the zip file in case imported via scanner           
            if (path.toLowerCase().contains("zip")) {
            	String pathZIP = path.substring(0, path.lastIndexOf("/") );
            	pathProd = pathZIP.substring(0, pathZIP.lastIndexOf("/") );
            }
            
            // open NetCDF file and read the relevant DS
            NetcdfDataset dsL2P = NetcdfDataset.openDataset(pathProd+ "/"+L2Pname);        
            Variable data = dsL2P.findVariable("sea_surface_temperature");
            // slice the D3 parameter in D2
            ArrayDouble.D3 dataArray = (D3) data.read();
            ArrayDouble.D2 dataArraySliced = (D2) dataArray.slice(0,0);
            // get the dmension
            int[] shapeArray = dataArraySliced.getShape();           
            int rows = shapeArray[1];
            int columns = shapeArray[0];
            int step = 1;
            if (columns > 10000)
            {
            	step=3;
            	rows = rows/3-1;
            	columns= columns/3-1;
            }
              //copy to output variable
            double[][] ds = new double[rows][columns];
            for (int index_rows = 0; index_rows < rows; index_rows++)
            {
                    for (int index_cols = 0; index_cols < columns; index_cols++)
                {                    
                    	ds[index_rows][index_cols] = (dataArraySliced.get(index_cols*step, index_rows*step));
                 }
            }
            dsL2P.close();
            if (path.toLowerCase().contains("zip")) {
            	boolean success = (new File(pathProd+ "/"+L2Pname)).delete();
            }

            return ds;
        }
        catch (Exception e)
        {
            LOGGER.error("L2P SST reading  failure.", e);
            return null;
        }
    }

    public static int[][] extractFlags(DrbCollectionImage sources)
    {
        try
        {
        	DrbImage image = sources.getChildren().iterator().next();
            DrbNode node = ((DrbNode) (image.getItemSource()));
              
// get path of the product that is injected           
            String path = node.getPath();
            String name = node.getName();

// get the file name of the product
            Query q = new Query("data(xfdumanifest.xml/XFDU/dataObjectSection/"
                    + "dataObject[@ID=\"L2P_Data\"]/byteStream/fileLocation/@href)");
            DrbSequence seq = q.evaluate(node);

            String L2Pnametmp = seq.getItem(0).getValue().toString();            
            String L2Pname = L2Pnametmp.substring(2);          
            String PathProd = path;

          //manage the zip file in case imported via scanner           
            if (path.toLowerCase().contains("zip")) {
            	String pathZIP = path.substring(0, path.lastIndexOf("/") );
            	PathProd = pathZIP.substring(0, pathZIP.lastIndexOf("/") );
          
            	FileOutputStream fileoutputstream;
            	ZipInputStream zip = new ZipInputStream(new FileInputStream(pathZIP));
            	ZipEntry zipEntry = null;
            	zipEntry = zip.getNextEntry( );
            	byte[] buf = new byte[1024];
            	int n;
            	while( ( zipEntry = zip.getNextEntry( ) ) != null )
            	{
            		if( zipEntry.getName( ).equals( name + "/"+ L2Pname ) )
            		{ 
            			fileoutputstream = new FileOutputStream(PathProd +"/"+L2Pname );
            			while ((n = zip.read(buf, 0, 1024)) > -1) {
            				fileoutputstream.write(buf, 0, n);
            			}
            			fileoutputstream.close();
            		}
            	}
            	zip.close();
            }
            // open NetCDF file and read the relevant DS
            //NetcdfDataset dsL2P = NetcdfDataset.openDataset(path+"/"+L2Pname);        
           	
            NetcdfDataset dsL2P = NetcdfDataset.openDataset(PathProd+"/"+L2Pname);        
            
           	Variable data = dsL2P.findVariable("l2p_flags");
            // slice the D3 parameter in D2
            ArrayShort.D3 dataArray = (ucar.ma2.ArrayShort.D3) data.read();
            ArrayShort.D2 dataArraySliced = (ucar.ma2.ArrayShort.D2) dataArray.slice(0,0);
            // get the dimension
            int[] shapeArray = dataArraySliced.getShape();           
            int rows = shapeArray[1];
            int columns = shapeArray[0];
            int step = 1;
            if (columns > 10000)
            {
            	step=3;
            	rows = rows/3-1;
            	columns= columns/3-1;
            }

            //copy to output variable
            int[][] ds = new int[rows][columns];
            for (int index_rows = 0; index_rows < rows; index_rows++)
            {
                    for (int index_cols = 0; index_cols < columns; index_cols++)
                {                    
                    	ds[index_rows][index_cols] = (int) (dataArraySliced.get(index_cols*step, index_rows*step));
                 }
            }
            dsL2P.close();
            return ds;
        }
        catch (Exception e)
        {
            LOGGER.error("L2P SST Flags reading failure.", e);
            return null;
        }
    }
 
    private boolean isMicro(int mask)
    {
        return (mask & 1) > 0;
    }

    private boolean isSnowIce(int mask)
    {
        return (mask & 4) > 0;
    }

    private boolean isGlint(int mask)
    {
        return (mask & 256) > 0;
    }

    private boolean isStrato(int mask)
    {
        return (mask & 8192) > 0;
    }
    
    private boolean isCloud(int mask)
    {
        return (mask & 512) > 0;
    }

    private boolean isLand(int mask)
    {
        return (mask & 2) > 0;
    }

    private boolean isFilled(int mask)
    {
        return (mask & 64) > 0;
    }

    private boolean isExceptions(int mask)
    {
        return (mask & 2048) > 0;
    }

    private boolean isOverflow(int mask)
    {
        return (mask & 4096) > 0;
    }
}
