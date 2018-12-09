// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import flow.modules.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import org.json.*;

/** 
    A collection of functions which build the application menu.
*/

public class AppMenu
    {
    public static final String PATCH_EXTENSION = ".flow";
    
    // Returns a menu for a given module class.
    static JMenuItem menuFor(Class moduleClass, Rack rack)
        {
        JMenuItem menu = new JMenuItem(Modulation.getNameForModulation(moduleClass));
        menu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.add(moduleClass);
                if (rack.getAddModulesAfter())  // need to move it
                    {
                    rack.move(rack.getAllModulePanels()[0], rack.getAllModulePanels().length - 2);
                    rack.scrollToRight();
                    }
                else
                    {
                    rack.scrollToLeft();
                    }
                rack.checkOrder();
                }
            });
        return menu;
        }
    
    // Returns the MIDI and Audio Preferences menu
    static JMenuItem setupPatchMenu(final Rack rack)
        {
        JMenuItem setup = new JMenuItem("MIDI and Audio Preferences");
        setup.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.chooseMIDIandAudio();
                }
            });
        return setup;
        }       

    // Returns the MIDI and Audio Preferences menu
    static JMenuItem setupTuningMenu(final Rack rack)
        {
        JMenuItem setup = new JMenuItem("Tuning Parameters");
        setup.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.chooseTuningParameters();
                }
            });
        return setup;
        }       

    // last file selected by open/save/save as
    static File file = null;

    /** The last directory used to open or save a file. */
    public static File dirFile = null;
        
    /** Returns a string which guarantees that the given filename ends with the given ending. */   
    public static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }


    static JMenuItem namePatchMenu(Rack rack)
        {
        JMenuItem name = new JMenuItem("Patch Info...");
        name.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        name.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                String[] result = Rack.showPatchDialog(rack, rack.getPatchName(), rack.getPatchAuthor(), rack.getPatchDate(), rack.getPatchVersion(), rack.getPatchInfo());
                rack.setPatchName(result[0]);
                rack.setPatchAuthor(result[1]);
                rack.setPatchDate(result[2]);
                rack.setPatchVersion(result[3]);
                rack.setPatchInfo(result[4]);
                }
            });
        return name;
        }


    static JMenuItem quitPatchMenu(Rack rack)
        {
        JMenuItem quit = new JMenuItem("Exit");

        quit.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.doQuit();
                }
            });
        return quit;
        }


 
    // Produces the Save Patch menu
    static JMenuItem savePatchMenu(Rack rack)
        {
        JMenuItem save = new JMenuItem("Save Patch");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                Modulation[] mods = new Modulation[rack.allModulePanels.size()];
                for(int i = 0; i < mods.length; i++)
                    {
                    mods[i] = rack.allModulePanels.get(i).getModulation();
                    }
                     
                if (file != null)
                    {
                    JSONObject obj = new JSONObject();
                    Sound.saveName(rack.getPatchName(), obj);
                    Sound.saveFlowVersion(obj);
                    Sound.savePatchVersion(rack.getPatchVersion(), obj);
                    Sound.savePatchInfo(rack.getPatchInfo(), obj);
                    Sound.savePatchAuthor(rack.getPatchAuthor(), obj);
                    Sound.savePatchDate(rack.getPatchDate(), obj);

                    PrintWriter p = null;

                        rack.output.lock();
                        try
                            {
                            rack.output.getSound(0).saveModules(obj);
                            p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
                            System.out.println(obj);
                            p.println(obj);
                            p.close();
                            }
                        catch (Exception e2)
                            {
                            e2.printStackTrace();
                            try { if (p != null) p.close(); }
                            catch (Exception e3) { }
                            }
                        finally 
                            {
                            rack.output.unlock();
                            }
                    }
                else
                    {
                    doSaveAs(rack);
                    }
                }
            });
        return save;
        }


    // Produces the Save Patch menu
    static JMenuItem saveAsPatchMenu(Rack rack)
        {
        JMenuItem save = new JMenuItem("Save Patch As...");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));
        save.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doSaveAs(rack);
                }
            });
        return save;
        }
    
        
    static void doSaveAs(Rack rack)
        {
        Modulation[] mods = new Modulation[rack.allModulePanels.size()];
        for(int i = 0; i < mods.length; i++)
            {
            mods[i] = rack.allModulePanels.get(i).getModulation();
            }
                     
        FileDialog fd = new FileDialog((Frame)(SwingUtilities.getRoot(rack)), "Save Patch to Sysex File...", FileDialog.SAVE);
                
        if (file != null)
            {
            fd.setFile(file.getName());
            // dirFile should always exist if file exists
            fd.setDirectory(file.getParentFile().getPath());
            }
        else
            {
            String name = rack.getPatchName();
            if (name == null) name = "Untitled";
            fd.setFile(name + PATCH_EXTENSION);
            if (dirFile != null)
                fd.setDirectory(dirFile.getParentFile().getPath());
            }

        rack.disableMenuBar();
        fd.setVisible(true);
        rack.enableMenuBar();
                
        File f = null; // make compiler happy

        PrintWriter p = null;
        if (fd.getFile() != null)
                {
                f = new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(), PATCH_EXTENSION));
                
                JSONObject obj = new JSONObject();
                if (rack.getPatchName() == null)
                    Sound.saveName(removeExtension(f.getName()), obj);
                else
                    Sound.saveName(rack.getPatchName(), obj);
                Sound.savePatchVersion(rack.getPatchVersion(), obj);
                Sound.savePatchInfo(rack.getPatchInfo(), obj);
                Sound.savePatchAuthor(rack.getPatchAuthor(), obj);
                Sound.saveFlowVersion(obj);

                rack.output.lock();
                try
                    {
                    rack.output.getSound(0).saveModules(obj);
                    p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(f)));
                    System.err.println(obj);
                    p.println(obj);
                    p.flush();
                    p.close();
                    }
                catch (Exception e)
                    {
                    e.printStackTrace();
                    try { if (p != null) p.close(); }
                    catch (Exception e2) { }
                    }
                finally 
                    {
                    rack.output.unlock();
                    }
                file = f;
                dirFile = f;
                rack.setPatchName(removeExtension(f.getName()));
                }
        }

// From https://stackoverflow.com/questions/924394/how-to-get-the-filename-without-the-extension-in-java
    public static String removeExtension(String fileName)
        {
        if (fileName.indexOf(".") > 0) 
            {
            return fileName.substring(0, fileName.lastIndexOf("."));
            }
        else 
            {
            return fileName;
            }
        }


    // Produces the Load Patch menu
    static JMenuItem loadPatchMenu(Rack rack)
        {
        JMenuItem load = new JMenuItem("Load Patch...");
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        load.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), "Load Patch File...", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter()
                    {
                    public boolean accept(File dir, String name)
                        {
                        return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                        }
                    });

                if (file != null)
                    {
                    fd.setFile(file.getName());
                    fd.setDirectory(file.getParentFile().getPath());
                    }
                else
                    {
                    }
                
                rack.disableMenuBar();
                fd.setVisible(true);
                rack.enableMenuBar();
                File f = null; // make compiler happy
                
                String[] patchName = new String[1];
                
                if (fd.getFile() != null)
                    //try
                    {
                    f = new File(fd.getDirectory(), fd.getFile());
                    rack.output.lock();
                    try
                        {
                        JSONObject obj = null;
                        int flowVersion = 0;
                        try 
                            { 
                            obj = new JSONObject(new JSONTokener(new GZIPInputStream(new FileInputStream(f)))); 
                            flowVersion = Sound.loadFlowVersion(obj);
                            }
                        catch (Exception ex) { ex.printStackTrace(); }
                        // version
                        try
                            {
                            Modulation[][] mods = new Modulation[rack.getOutput().getNumSounds()][];
                            for(int i = 0; i < mods.length; i++)
                                {
                                mods[i] = Sound.loadModules(obj, flowVersion);
                                }
                                                                                                
                            // Create and update Modulations and create ModulePanels
                            load(mods, rack, obj == null ? patchName[0] : Sound.loadName(obj));
                            if (obj != null)
                            	{
                            	rack.setPatchVersion(Sound.loadPatchVersion(obj));
                            	rack.setPatchInfo(Sound.loadPatchInfo(obj));
                            	rack.setPatchAuthor(Sound.loadPatchAuthor(obj));
                            	rack.setPatchDate(Sound.loadPatchDate(obj));
                            	}
                            rack.checkOrder();
                            }
                        finally 
                            {
                            rack.output.unlock();
                            }
                        }
                    catch(Exception ex) { ex.printStackTrace(); showSimpleError("Patch Reading Error", "The patch could not be loaded", rack); }
                    file = f;
                    dirFile = f;
                    }
                }
            });
        return load;
        }


    // Produces the Load Patch as Macro menu
    static JMenuItem loadMacroMenu(Rack rack)
        {
        JMenuItem macro = new JMenuItem("Load Patch as Macro...");
        macro.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), "Load Patch File as Macro...", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter()
                    {
                    public boolean accept(File dir, String name)
                        {
                        return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                        }
                    });

                if (file != null)
                    {
                    fd.setFile(file.getName());
                    fd.setDirectory(file.getParentFile().getPath());
                    }
                else
                    {
                    }
                
                rack.disableMenuBar();
                fd.setVisible(true);
                rack.enableMenuBar();
                File f = null; // make compiler happy
                if (fd.getFile() != null)
                    //try
                    {
                    f = new File(fd.getDirectory(), fd.getFile());
                    rack.output.lock();
                    try
                        {
                        rack.addMacro(f);
                        if (rack.getAddModulesAfter())  // need to move it
                            {
                            rack.move(rack.getAllModulePanels()[0], rack.getAllModulePanels().length - 2);
                            rack.scrollToRight();
                            }
                        else
                            {
                            rack.scrollToLeft();
                            }
                        rack.checkOrder();
                        }
                    finally 
                        {
                        rack.output.unlock();
                        }
                    dirFile = f;
                    }
                }
            });
        return macro;
        }
        

    static boolean inSimpleError;

    /** Display a simple error message. */
    public static void showSimpleError(String title, String message, Rack rack)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        rack.disableMenuBar();
        JOptionPane.showMessageDialog(rack, message, title, JOptionPane.ERROR_MESSAGE);
        rack.enableMenuBar();
        inSimpleError = false;
        }

    /** Display a simple error message. */
    public static void showSimpleMessage(String title, String message, Rack rack)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        rack.disableMenuBar();
        JOptionPane.showMessageDialog(rack, message, title, JOptionPane.INFORMATION_MESSAGE);
        rack.enableMenuBar();
        inSimpleError = false;
        }


    // Display a simple (OK / Cancel) confirmation message.  Return the result (ok = true, cancel = false).
    static boolean showSimpleConfirm(String title, String message, Rack rack)
        {
        rack.disableMenuBar();
        boolean result = (JOptionPane.showConfirmDialog(rack, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.OK_OPTION);
        rack.enableMenuBar();
        return result;
        }


    // Produces the New Patch menu
    static JMenuItem newPatchMenu(Rack rack)
        {
        JMenuItem newpatch = new JMenuItem("New Patch");
        newpatch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        newpatch.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (showSimpleConfirm("New Patch", "Clear the existing patch?", rack))
                    {
                    rack.output.lock();
                    try
                        {
                        rack.closeAll();
                        rack.checkOrder();
                        rack.add(Out.class);
                        rack.setPatchName(null);
                        file = null;
                        // don't reset dirFile
                        }
                    finally 
                        {
                        rack.output.unlock();
                        }
                    }
                }
            });
        return newpatch;
        }
        

    static JMenuItem waterfallDisplay(Rack rack)
        {
        final JCheckBoxMenuItem waterfall = new JCheckBoxMenuItem("Waterfall Display");
        waterfall.setSelected(Prefs.getWaterfallDisplay());
        rack.display1.setWaterfall(waterfall.isSelected());
        rack.display2.setWaterfall(waterfall.isSelected());
        waterfall.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
				rack.display1.setWaterfall(waterfall.isSelected());
				rack.display2.setWaterfall(waterfall.isSelected());
                Prefs.setWaterfallDisplay(waterfall.isSelected());
                }
            });
        return waterfall;
        }

    static JMenuItem logAxisDisplay(Rack rack)
        {
        final JCheckBoxMenuItem log = new JCheckBoxMenuItem("Log-Axis Display");
        log.setSelected(Prefs.getLogAxisDisplay());
        rack.display1.setLogFrequency(log.isSelected());
        rack.display2.setLogFrequency(log.isSelected());
        log.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
				rack.display1.setLogFrequency(log.isSelected());
				rack.display2.setLogFrequency(log.isSelected());
                Prefs.setLogAxisDisplay(log.isSelected());
                }
            });
        return log;
        }

	public static final int DEFAULT_MAX_DISPLAYED_HARMONIC = 6;		// 150
	
    static JMenuItem maxDisplayedHarmonic(Rack rack)
        {
        final double maxHarm[] = new double[] { 31, 49, 63, 79, 99, 127, 149, 199, 255, 299, 399, 499 };
        final JMenu max = new JMenu("Max Displayed Harmonic");
        final JRadioButtonMenuItem[] buttons = new JRadioButtonMenuItem[] {
        new JRadioButtonMenuItem("32"),
        new JRadioButtonMenuItem("50"),
        new JRadioButtonMenuItem("64"),
        new JRadioButtonMenuItem("80"),
        new JRadioButtonMenuItem("100"),
        new JRadioButtonMenuItem("128"),
        new JRadioButtonMenuItem("150"),
        new JRadioButtonMenuItem("200"),
        new JRadioButtonMenuItem("256"),
        new JRadioButtonMenuItem("300"),
        new JRadioButtonMenuItem("400"),
        new JRadioButtonMenuItem("500")};
        
        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < buttons.length; i++)
        	{
        	max.add(buttons[i]);
        	group.add(buttons[i]);
        	}
        
        int sel = Prefs.getMaxDisplayedHarmonic();
        buttons[sel].setSelected(true);
		rack.display1.setMaxFrequency(maxHarm[sel]);
		rack.display2.setMaxFrequency(maxHarm[sel]);
		
		for(int q = 0; q < buttons.length; q++)
		{
        buttons[q].addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // yuck
                int selected = 0;
                for(int i = 0; i < buttons.length; i++)
                	if (buttons[i].isSelected()) 
                		{ 
                		selected = i; 
                		break; 
                		}
                	
				rack.display1.setMaxFrequency(maxHarm[selected]);
				rack.display2.setMaxFrequency(maxHarm[selected]);
                Prefs.setMaxDisplayedHarmonic(selected);
                }
            });
        }
        return max;
        }


	public static final int DEFAULT_MIN_DISPLAYED_HARMONIC = 4;		// 1/16
	
    static JMenuItem minDisplayedHarmonic(Rack rack)
        {
        final double minHarm[] = new double[] { 1.0, 0.5, 0.25, 0.125, 0.0625, 0.03125 };
        final JMenu min = new JMenu("Min Displayed Harmonic");
        final JRadioButtonMenuItem[] buttons = new JRadioButtonMenuItem[] {
        new JRadioButtonMenuItem("Fundamental"),
        new JRadioButtonMenuItem("1/2"),
        new JRadioButtonMenuItem("1/4"),
        new JRadioButtonMenuItem("1/8"),
        new JRadioButtonMenuItem("1/16"),
        new JRadioButtonMenuItem("1/32") };
        
        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < buttons.length; i++)
        	{
        	min.add(buttons[i]);
        	group.add(buttons[i]);
        	}
        
        int sel = Prefs.getMinDisplayedHarmonic();
        buttons[sel].setSelected(true);
		rack.display1.setMinFrequency(minHarm[sel]);
		rack.display2.setMinFrequency(minHarm[sel]);
        for(int q = 0; q < buttons.length; q++)
		{
		buttons[q].addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // yuck
                int selected = 0;
                for(int i = 0; i < buttons.length; i++)
                	if (buttons[i].isSelected()) 
                		{ 
                		selected = i; 
                		break; 
                		}
                	
				rack.display1.setMinFrequency(minHarm[selected]);
				rack.display2.setMinFrequency(minHarm[selected]);
                Prefs.setMinDisplayedHarmonic(selected);
                }
            });
        }
        return min;
        }


    static JMenuItem playFirstMenu(Rack rack)
        {
        final JCheckBoxMenuItem playFirst = new JCheckBoxMenuItem("Monophonic");
        playFirst.setSelected(Prefs.getLastOneVoice());
        rack.getOutput().setOnlyPlayFirstSound(playFirst.isSelected());
        playFirst.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().setOnlyPlayFirstSound(playFirst.isSelected());
                Prefs.setLastOneVoice(playFirst.isSelected());
                }
            });
        return playFirst;
        }


    // Produces the Add New Modules At End menu
    static JMenuItem addModulesAfterMenu(Rack rack)
        {
        final JCheckBoxMenuItem addModulesAfter = new JCheckBoxMenuItem("Add New Modules At End");
        addModulesAfter.setSelected(Prefs.getLastAddModulesAfter());
        rack.setAddModulesAfter(addModulesAfter.isSelected());
        addModulesAfter.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.setAddModulesAfter(addModulesAfter.isSelected());
                Prefs.setLastAddModulesAfter(addModulesAfter.isSelected());
                }
            });
        return addModulesAfter;
        }


    // Produces the Sync to MIDI Clock menu
    static JMenuItem syncMenu(Rack rack)
        {
        final JCheckBoxMenuItem sync = new JCheckBoxMenuItem("Sync to MIDI Clock");
        sync.setSelected(Prefs.getLastMIDISync());
        rack.getOutput().getInput().getMidiClock().setSyncing(sync.isSelected());
        sync.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().getInput().getMidiClock().setSyncing(sync.isSelected());
                Prefs.setLastMIDISync(sync.isSelected());
                }
            });
        return sync;
        }

	public static JMenuBar provideMenuBar(Rack rack)
		{
        JMenuBar menubar = new JMenuBar();
        menubar.add(provideFileMenu(rack));
        menubar.add(providePlayMenu(rack));
        menubar.add(provideModuleMenu(rack));
        menubar.add(provideOptionsMenu(rack));
        if (Style.isWindows() || Style.isUnix())
            {
            menubar.add(AppMenu.provideWindowsAboutMenu(rack));
            }
        return menubar;
		}

    static JMenuItem resetMenu(Rack rack)
        {
        JMenuItem reset = new JMenuItem("Reset");
        reset.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.reset();
                }
            });
        return reset;
        }

    // Produces the Play menu
    public static JMenu providePlayMenu(Rack rack)
        {
        JMenu menu = new JMenu("Play");

        menu.add(resetMenu(rack));
        menu.add(playFirstMenu(rack));
        menu.add(syncMenu(rack));
        menu.addSeparator();
        menu.add(namePatchMenu(rack));
        return menu;
        }

    // Produces the Play menu
    public static JMenu provideOptionsMenu(Rack rack)
        {
        JMenu menu = new JMenu("Options");

        menu.add(logAxisDisplay(rack));
        menu.add(waterfallDisplay(rack));
        menu.add(maxDisplayedHarmonic(rack));
        menu.add(minDisplayedHarmonic(rack));
        menu.addSeparator();
        menu.add(addModulesAfterMenu(rack));
        menu.add(setupPatchMenu(rack));
        menu.add(setupTuningMenu(rack));
        return menu;
        }

    // Produces the File menu
    public static JMenu provideFileMenu(Rack rack)
        {
        JMenu menu = new JMenu("File");

        menu.add(newPatchMenu(rack));
        menu.add(savePatchMenu(rack));
        menu.add(saveAsPatchMenu(rack));
        menu.add(loadPatchMenu(rack));

        if (!Style.isMac())
            {
            menu.addSeparator();
            menu.add(quitPatchMenu(rack));
            }

        return menu;
        }

    // Produces the Module menu
    public static JMenu provideModuleMenu(Rack rack)
        {
        JMenu menu = new JMenu("Modules");

        menu.add(loadMacroMenu(rack));
        
        ArrayList<JMenuItem> modSources = new ArrayList<>();
        ArrayList<JMenuItem> modShapers = new ArrayList<>();
        ArrayList<JMenuItem> unitSources = new ArrayList<>();
        ArrayList<JMenuItem> unitShapers = new ArrayList<>();
        //JMenuItem outMenu = null;
        JMenuItem inMenu = null;

		Class[] modules = Modules.getModules();
        for(int i = 0; i < modules.length; i++)
            {
            Class c = modules[i];
            JMenuItem m = menuFor(c, rack);
                        
            if (c == flow.modules.Out.class)
                { } // do nothing //outMenu = m;
            else if (c == flow.modules.In.class)
                inMenu = m;
            else if (flow.UnitSource.class.isAssignableFrom(c))
                unitSources.add(m);
            else if (flow.ModSource.class.isAssignableFrom(c))
                modSources.add(m);
            else if (flow.Unit.class.isAssignableFrom(c))
                unitShapers.add(m);
            else  // Module
                modShapers.add(m);
            }
                
        //menu.add(outMenu);
        menu.add(inMenu);
                
        JMenu sub = new JMenu("Modulation Sources");
        for(JMenuItem m : modSources)
            sub.add(m);
        menu.add(sub);

        sub = new JMenu("Modulation Shapers");
        for(JMenuItem m : modShapers)
            sub.add(m);
        menu.add(sub);

        sub = new JMenu("Partials Sources");
        for(JMenuItem m : unitSources)
            sub.add(m);
        menu.add(sub);

        sub = new JMenu("Partials Shapers");
        for(JMenuItem m : unitShapers)
            sub.add(m);
        menu.add(sub);
                        
        return menu;
        }


    // Removes all modules from the rack, and
    // loads new modules from the given deserialized array.  The Modulations
    // are organized by Sound, then by Modulation.
    static void load(Modulation[][] mods, Rack rack, String patchName)
        {
        Output output = rack.getOutput();
        rack.output.lock();
        try
            {
            // remove all existing panels
            rack.closeAll();
            rack.checkOrder();
                   
            // Add the modulations as a group
            for(int i = 0; i < mods.length; i++)
                {
                Sound sound = output.getSound(i);
                for(int j = 0; j < mods[i].length; j++)
                    {
                    sound.register(mods[i][j]);
                    mods[i][j].setSound(sound);
                    if (mods[i][j] instanceof Out)
                        {
                        sound.setEmits((Out)(mods[i][j]));
                        }
                    mods[i][j].reset();
                    }
                }
                                
            // Load ModulePanels for the new Modulations
            for(int j = 0; j < mods[0].length; j++)
                {
                ModulePanel modpanel = mods[0][j].getPanel();
                rack.addModulePanel(modpanel);
                }

            rack.setPatchName(patchName);

            // Connect and update ModulePanels
            rack.rebuild();
            rack.checkOrder();
            rack.setPatchName(rack.getPatchName());
            }
        finally 
            {
            rack.output.unlock();
            }
        }

    public static JMenu provideWindowsAboutMenu(Rack rack)
        {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About Flow");
        aboutMenuItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doAbout();
                }
            });
        helpMenu.add(aboutMenuItem);
        return helpMenu;
        }
                
    static void doAbout()
        {
        ImageIcon icon = new ImageIcon(AppMenu.class.getResource("About.png"));
        JFrame frame = new JFrame("About Flow");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.BLACK);
        JLabel label = new JLabel(icon);
//        label.setBorder(BorderFactory.createMatteBorder(Color.GRAY, 4));
        frame.getContentPane().add(label, BorderLayout.CENTER);

        JPanel pane = new JPanel()
            {
            public Insets getInsets() { return new Insets(10, 10, 10, 10); }
            };
        pane.setBackground(Color.BLACK);
        pane.setLayout(new BorderLayout());

        JLabel edisyn = new JLabel("Flow");
        edisyn.setForeground(Color.WHITE);
        edisyn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        pane.add(edisyn, BorderLayout.WEST);

        Box box = new Box(BoxLayout.Y_AXIS);
        JLabel about = new JLabel("Version " + Flow.VERSION + " By Sean Luke");
        about.setForeground(Color.WHITE);
        about.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JLabel about2 = new JLabel("Copyright 2018 George Mason University");
        about2.setForeground(Color.WHITE);
        about2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JLabel about3 = new JLabel("http://github.com/eclab/flow/");
        about3.setForeground(Color.WHITE);
        about3.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        box.add(about);
        box.add(about2);
        box.add(about3);
        
        pane.add(box, BorderLayout.EAST);

        frame.add(pane, BorderLayout.SOUTH);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        }
    }
