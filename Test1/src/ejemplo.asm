format PE CONSOLE 4.0
entry start

include 'win32a.inc'

section '.data' data readable writeable

       str_pause db  'pause' ,0
        @intprintstr db 'Resultado: %d' ,10,0
     @intscanstr db '%d',0
		


section '.code' code readable executable

  start: 
		mov eax,1
		mov [@a@],eax
		
		for1:
		mov eax, 5
		cmp [@a@], eax
		jge end_for1
		inc [@a@]
		jmp for1
		
		end_for1:
    
;Finalizar el proceso
      push str_pause
      call [system]
      add esp, 4
      call [ExitProcess]   

section '.idata' import data readable writeable

  library kernel,'KERNEL32.DLL',\
          ms ,'msvcrt.dll'

  import kernel,\
         ExitProcess,'ExitProcess'

  import ms,\
         printf,'printf',\
         cget ,'_getch',\
         system,'system',\
         scanf,'scanf'
                          