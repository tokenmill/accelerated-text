import Block                from './Block';
import { red as color }     from './colors.sass';
import * as T               from './types';
import TwoInputs            from './icons/TwoInputs';


export default Block({

    type:                   'RST',
    color,
    icon:                   TwoInputs({ color }),

    json: {

        colour:             color,
        output:             T.STRING,
        message0:           '%1',
        args0: [{
            type:           'field_dropdown',
            name:           'rstType',
            options: [
                [ 'Perception', 'perception' ],
                [ 'Consequence', 'consequence' ],
            ],
        }],
        message1:           'lexicon: %1',
        args1: [{
            type:           'input_value',
            name:           'lexicon',
            check:          T.TEXT,
        }],
        message2:           'nucleus: %1',
        args2: [{
            type:           'input_value',
            name:           'nucleus',
            check:          T.TEXT,
        }],
        message3:           'satellite: %1',
        args3: [{
            type:           'input_value',
            name:           'satellite',
            check:          T.TEXT,
        }],
    },
});
