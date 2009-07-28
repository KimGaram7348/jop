library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

use work.sc_pack.all;

entity datacache is
port (
	clk, reset:	    in std_logic;

	inval:			in std_logic;

	cpu_out:		in sc_out_type;
	cpu_in:			out sc_in_type;

	mem_out:		out sc_out_type;
	mem_in:			in sc_in_type);
end datacache;

architecture rtl of datacache is

	type mux_type is (bp, dm, fa);
	signal out_mux_reg, next_out_mux : mux_type;	
	signal in_mux_reg, next_in_mux : mux_type;	
	
	signal bp_cpu_in, dm_cpu_in, fa_cpu_in : sc_in_type;
	signal bp_mem_out, dm_mem_out, fa_mem_out : sc_out_type;
	
begin  -- rtl

	cmp_bypass: entity work.null_cache
		port map (
			clk		=> clk,
			reset	=> reset,
			inval	=> inval,
			cpu_in	=> bp_cpu_in,
			cpu_out => cpu_out,
			mem_in	=> mem_in,
			mem_out => bp_mem_out);
	
	cmp_dm: entity work.directmapped
		port map (
			clk		=> clk,
			reset	=> reset,
			inval	=> inval,
			cpu_in	=> dm_cpu_in,
			cpu_out => cpu_out,
			mem_in	=> mem_in,
			mem_out => dm_mem_out);

	cmp_fa: entity work.lru
		port map (
			clk		=> clk,
			reset	=> reset,
			inval	=> inval,
			cpu_in	=> fa_cpu_in,
			cpu_out => cpu_out,
			mem_in	=> mem_in,
			mem_out => fa_mem_out);

	sync: process (clk, reset)
	begin  -- process sync
		if reset = '1' then  			-- asynchronous reset (active low)
			out_mux_reg <= bp;
			in_mux_reg <= bp;
		elsif clk'event and clk = '1' then  -- rising clock edge
			out_mux_reg <= next_out_mux;
			in_mux_reg <= next_in_mux;
		end if;
	end process sync;

	async: process (cpu_out, mem_in,
					out_mux_reg, in_mux_reg,
					bp_mem_out, dm_mem_out, fa_mem_out,
					bp_cpu_in, dm_cpu_in, fa_cpu_in)
	begin  -- process async
		
		next_out_mux <= out_mux_reg;
		next_in_mux <= in_mux_reg;

		case out_mux_reg is
			when dm =>
				mem_out <= dm_mem_out;
				cpu_in.rdy_cnt <= dm_cpu_in.rdy_cnt;
			when fa =>
				mem_out <= fa_mem_out;					   
				cpu_in.rdy_cnt <= fa_cpu_in.rdy_cnt;
			when others =>
				mem_out <= bp_mem_out;
				cpu_in.rdy_cnt <= bp_cpu_in.rdy_cnt;
		end case;

		if cpu_out.rd = '1' or cpu_out.wr = '1' then
			case cpu_out.cache is
				when direct_mapped =>
					mem_out <= dm_mem_out;
					next_out_mux <= dm;									  
				when full_assoc =>
					mem_out <= fa_mem_out;
					next_out_mux <= fa;
				when others =>
					mem_out <= bp_mem_out;
					next_out_mux <= bp;
			end case;
		end if;

		case in_mux_reg is
			when dm =>
				cpu_in.rd_data <= dm_cpu_in.rd_data;
			when fa =>
				cpu_in.rd_data <= fa_cpu_in.rd_data;					   
			when others =>
				cpu_in.rd_data <= bp_cpu_in.rd_data;
		end case;

		if out_mux_reg = bp and bp_cpu_in.rdy_cnt(1) = '0' then
			next_in_mux <= bp;
		end if;
		if out_mux_reg = dm and dm_cpu_in.rdy_cnt(1) = '0' then
			next_in_mux <= dm;
		end if;
		if out_mux_reg = fa and fa_cpu_in.rdy_cnt(1) = '0' then
			next_in_mux <= fa;
		end if;

		if out_mux_reg = bp and bp_cpu_in.rdy_cnt = "00" then
			cpu_in.rd_data <= bp_cpu_in.rd_data;
		end if;
		if out_mux_reg = dm and dm_cpu_in.rdy_cnt = "00" then
			cpu_in.rd_data <= dm_cpu_in.rd_data;
		end if;
		if out_mux_reg = fa and fa_cpu_in.rdy_cnt = "00" then
			cpu_in.rd_data <= fa_cpu_in.rd_data;
		end if;

	end process async;
	
end rtl;
