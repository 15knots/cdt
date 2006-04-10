#!/usr/bin/perl -w

use Shell;

if (!defined($ARGV[0]) || !defined($ARGV[1]) || !defined($ARGV[2]) || !defined($ARGV[3]) || !defined($ARGV[4]))
{
    print("command usage:\n");
   print("auth.pl USER, PATH, PORT, TIMEOUT, TICKET\n");
}
else
{
  $userIN    = $ARGV[0];
  $pathIN    = $ARGV[1];
  $portIN    = $ARGV[2];
  $timeoutIN = $ARGV[3];
  $ticketIN  = $ARGV[4];

  $pwdIN = <STDIN>;
  chomp($pwdIN);


  @passwdStruct = getpwnam($userIN);

  if (@passwdStruct == 0)
  {
     print("invalid user name\n");
     0;
  }
  else
  {
    $passwd=$passwdStruct[1];
    $encryptedPWD = crypt($pwdIN, $passwd);
    $classpath=$ENV{CLASSPATH};
    $suOptions="-lp";

    if ($passwd eq $encryptedPWD)
    {
		print("success\n");

		$os = uname();
		chomp($os);

		if (lc($os) eq "aix")
		{
			$suOptions="-";
		}

		system("su $suOptions $userIN -c 'java -cp $classpath -DA_PLUGIN_PATH=$pathIN com.ibm.etools.systems.dstore.core.server.Server $portIN $timeoutIN $ticketIN'");
		1;
    }
    else
    {
		print("incorrect password\n");
    	0;
    }
  }
}
