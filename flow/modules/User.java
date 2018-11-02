// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/**
   A Unit which shifts the frequency of all partials.  There are three
   options:
   
   <ol>
   <li>Pitch.  Shift all partials by a certain pitch (for example, up an octave).
   This multiplies their frequencies by a certain amount.
   <li>Frequency.  Add a certain amount to the frequency of all partials.
   partials based on their distance from the nearest
   <li>Partials.  Move all partials towards the frequency of the next partial.
   </ol>
   
   The degree of shifting depends on the SHIFT modulation, bounded by the
   BOUND modulation.
*/

public class User extends Modulation
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_MODULATIONS = 4;
        
    public User(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO},  new String[] { "A", "B", "C", "D" });
        defineModulationOutputs(new String[] { "", "", "", "" });
        }
        
    boolean[] trigger = new boolean[NUM_MODULATIONS];
        
    public void go()
        {
        super.go();
        
        for(int i = 0; i < NUM_MODULATIONS; i++)
            {
            setModulationOutput(i, modulate(i));
            if (isTriggered(i) || trigger[i]) updateTrigger(i);
            trigger[i] = false;
            }
        }       

    public ModulePanel getPanel()
        {
        return new ModulePanel(User.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_MODULATIONS; i++)
                    {
                    final int _i = i;

                    Box hbox = new Box(BoxLayout.X_AXIS);
                    ModulationOutput output = new ModulationOutput(mod, i, this);
                    ModulationInput input = new ModulationInput(mod, i, this);
                    hbox.add(input);
                    hbox.add(output);
                    box.add(hbox);
                    }

                for(int i = 0; i < NUM_MODULATIONS; i++)
                    {
                    final int _i = i;
                    PushButton button = new PushButton("Tr " + i)
                        {
                        public void perform()
                            {
                            Output output = getRack().getOutput();
                            output.lock();
                            int numSounds = output.getNumSounds();
                            try
                                {
                                int index = sound.findRegistered(User.this);
                                for(int s = 0; s < numSounds; s++)
                                    {
                                    User user = (User)(output.getSound(s).getRegistered(index));
                                    user.trigger[_i] = true;
                                    }
                                }
                            finally 
                                {
                                output.unlock();
                                }
                            }
                        };
                    box.add(button);
                    }
                return box;
                }
            };
        }
    }
