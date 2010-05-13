/*
    Copyright (c) 1996-2007 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/RegexFunction.java#1 $

    Responsible: dfinlay
*/
package ariba.util.core;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
    @aribaapi ariba
*/
public class RegexFunction extends Function<String>
{
    public static enum Instruction {
        FullMatch,
        Find
    }

    //--------------------------------------------------------------------------
    // constants

    public static final int DefaultGroupNumber = 1;
    public static final Instruction DefaultInstruction = Instruction.Find;

    //--------------------------------------------------------------------------
    // data members

    private Pattern _pattern;
    private int _groupNumber;
    private Instruction _instruction;

    public RegexFunction (Pattern pattern, int groupNumber, Instruction instruction)
    {
        _pattern = pattern;
        _groupNumber = groupNumber;
        _instruction = DefaultInstruction;
    }

    public RegexFunction (Pattern pattern, int groupNumber)
    {
        _pattern = pattern;
        _groupNumber = groupNumber;
        _instruction = DefaultInstruction;
    }

    public RegexFunction (Pattern pattern)
    {
        this(pattern, DefaultGroupNumber);
    }

    public final String evaluate (Object... arguments)
    {
        return extract((String)arguments[0]);
    }

    public final String extract (String string)
    {
        Matcher matcher = _pattern.matcher(string);
        boolean matches  = (_instruction == Instruction.Find) ? matcher.find() : matcher.matches();
        return matches ? matcher.group(_groupNumber) : null;
    }
}
